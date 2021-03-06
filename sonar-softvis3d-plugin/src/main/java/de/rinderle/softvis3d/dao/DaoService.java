/**
 * SoftVis3D Sonar plugin
 * Copyright (C) 2016 Stefan Rinderle and Yvo Niedrich
 * stefan@rinderle.info / yvo.niedrich@gmail.com
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
package de.rinderle.softvis3d.dao;

import com.google.inject.Inject;
import de.rinderle.softvis3d.domain.VisualizationRequest;
import de.rinderle.softvis3d.domain.sonar.SonarMeasure;

import java.util.List;

import org.apache.commons.lang.time.StopWatch;
import org.sonar.api.server.ws.LocalConnector;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsMeasures;

public class DaoService {

  @Inject
  private SonarDao sonarDao;
  @Inject
  private DaoServiceTransformer daoServiceTransformer;

  public String getProjectId(final LocalConnector localConnector, String projectKey) {
    return this.sonarDao.getProjectId(localConnector, projectKey);
  }

  public List<SonarMeasure> getSubProjects(final LocalConnector localConnector, final String projectId) {
    final List<WsComponents.Component> resultComponents = this.sonarDao.getDirectModuleChildrenIds(localConnector, projectId);

    return daoServiceTransformer.transformComponentToModules(resultComponents);
  }

  public List<SonarMeasure> getFlatChildrenWithMetrics(final LocalConnector localConnector, final VisualizationRequest requestDTO) {
    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    final List<WsMeasures.Component> resultComponents = sonarDao.getAllSnapshotIdsWithRescourceId(localConnector,
      requestDTO.getRootSnapshotKey(), requestDTO.getMetrics());

    return daoServiceTransformer.transformComponentToMeasure(resultComponents);
  }

}
