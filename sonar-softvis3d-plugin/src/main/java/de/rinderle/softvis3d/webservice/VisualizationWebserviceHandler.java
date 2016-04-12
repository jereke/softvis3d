/*
 * SoftVis3D Sonar plugin
 * Copyright (C) 2015 Stefan Rinderle
 * stefan@rinderle.info
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package de.rinderle.softvis3d.webservice;

import com.google.inject.Inject;
import de.rinderle.softvis3d.SoftVis3DPlugin;
import de.rinderle.softvis3d.base.VisualizationAdditionalInfos;
import de.rinderle.softvis3d.base.VisualizationProcessor;
import de.rinderle.softvis3d.base.VisualizationSettings;
import de.rinderle.softvis3d.base.domain.LayoutViewType;
import de.rinderle.softvis3d.base.domain.MinMaxValue;
import de.rinderle.softvis3d.base.domain.SnapshotTreeResult;
import de.rinderle.softvis3d.base.domain.graph.ResultPlatform;
import de.rinderle.softvis3d.base.domain.tree.RootTreeNode;
import de.rinderle.softvis3d.base.layout.dot.DotExecutorException;
import de.rinderle.softvis3d.base.result.SoftVis3dJsonWriter;
import de.rinderle.softvis3d.base.result.TreeNodeJsonWriter;
import de.rinderle.softvis3d.base.result.VisualizationJsonWriter;
import de.rinderle.softvis3d.cache.LayoutCacheService;
import de.rinderle.softvis3d.dao.DaoService;
import de.rinderle.softvis3d.domain.SnapshotStorageKey;
import de.rinderle.softvis3d.domain.VisualizationRequest;
import de.rinderle.softvis3d.domain.sonar.ScmInfoType;
import de.rinderle.softvis3d.preprocessing.PreProcessor;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;

public class VisualizationWebserviceHandler extends AbstractWebserviceHandler implements RequestHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(VisualizationWebserviceHandler.class);

  private VisualizationSettings visualizationSettings;

  @Inject
  private VisualizationProcessor visualizationProcessor;
  @Inject
  private LayoutCacheService layoutCacheService;
  @Inject
  private PreProcessor preProcessor;
  @Inject
  private DaoService daoService;

  @Inject
  private TreeNodeJsonWriter treeNodeJsonWriter;
  @Inject
  private VisualizationJsonWriter visualizationJsonWriter;


  @Override
  public void handleRequest(final Request request, final Response response) throws Exception {
    final String projectKey = request.param("projectKey");

    final String projectId = daoService.getProjectId(request.getLocalConnector(), projectKey);

    final String footprintMetricKey = request.param("footprintMetricKey");
    final String heightMetricKey = request.param("heightMetricKey");

    // TODO: get metric key and check if its a special spftvis3d key. If not, just use the
    // metric key.
    final String colorMetricKey = request.param("colorMetricKey");
    final ScmInfoType scmInfoType = ScmInfoType.NONE;

    final VisualizationRequest requestDTO =
      new VisualizationRequest(projectId, footprintMetricKey, heightMetricKey, scmInfoType);

    LOGGER.info("VisualizationWebserviceHandler " + requestDTO.toString());

    final SnapshotStorageKey key = new SnapshotStorageKey(requestDTO);

    final SnapshotTreeResult snapshotTreeResult = preProcessor.process(request.getLocalConnector(), requestDTO);

    final Map<String, ResultPlatform> visualizationResult;
    if (SoftVis3DPlugin.CACHE_ENABLED && layoutCacheService.containsKey(key)) {
      LOGGER.info("Layout out of cache for " + key.toString());
      visualizationResult = layoutCacheService.getLayoutResult(key);
    } else {
      LOGGER.info("Create layout for " + key.toString());
      visualizationResult = createLayout(requestDTO, snapshotTreeResult);
      if (SoftVis3DPlugin.CACHE_ENABLED) {
        layoutCacheService.save(key, visualizationResult);
      }
    }

    this.writeResultsToResponse(response, snapshotTreeResult, visualizationResult);
  }

  private void writeResultsToResponse(final Response response, final SnapshotTreeResult snapshotTreeResult,
    final Map<String, ResultPlatform> visualizationResult) {

    final SoftVis3dJsonWriter jsonWriter = new SoftVis3dJsonWriter(response.stream().output());

    jsonWriter.beginObject();
    jsonWriter.name("resultObject");

    jsonWriter.beginArray();

    this.treeNodeJsonWriter.transformRootTreeToJson(jsonWriter, snapshotTreeResult.getTree());
    this.visualizationJsonWriter.transformResponseToJson(jsonWriter, visualizationResult);

    jsonWriter.endArray();

    jsonWriter.endObject();

    jsonWriter.close();
  }

  private Map<String, ResultPlatform> createLayout(final VisualizationRequest requestDTO,
    final SnapshotTreeResult snapshotTreeResult) throws DotExecutorException {
    logStartOfCalc(requestDTO);

    final Map<String, ResultPlatform> result = visualizationProcessor.visualize(requestDTO.getViewType(), this.visualizationSettings, snapshotTreeResult,
      createAdditionalInfos(snapshotTreeResult.getTree()));

    /**
     * Remove root layer in dependency view TODO: I don't know how to do this anywhere else.
     */
    if (requestDTO.getViewType().equals(LayoutViewType.DEPENDENCY)) {
      result.remove(requestDTO.getRootSnapshotKey());
    }

    return result;
  }

  private VisualizationAdditionalInfos createAdditionalInfos(RootTreeNode tree) {
    final MinMaxCalculator minMaxCalculator = new MinMaxCalculator(tree);

    final MinMaxValue minMaxMetricFootprint = minMaxCalculator.getMinMaxForFootprintMetric();
    final MinMaxValue minMaxMetricHeight = minMaxCalculator.getMinMaxForHeightMetric();

//     TODO
//    final MinMaxValue minMaxMetricColor = daoService.getMaxScmInfo(localConnector, requestDTO);
    final MinMaxValue minMaxMetricColor = new MinMaxValue(0, 100);

    return new VisualizationAdditionalInfos(minMaxMetricFootprint, minMaxMetricHeight, minMaxMetricColor);
  }

  private void logStartOfCalc(final VisualizationRequest visualizationRequest) {
    LOGGER.info("Start layout calculation for snapshot " + visualizationRequest.getRootSnapshotKey() + ", "
      + "metrics " + visualizationRequest.getHeightMetricKey() + " and "
      + visualizationRequest.getFootprintMetricKey());
  }

  public void setSettings(final VisualizationSettings settings) {
    this.visualizationSettings = settings;
  }

}
