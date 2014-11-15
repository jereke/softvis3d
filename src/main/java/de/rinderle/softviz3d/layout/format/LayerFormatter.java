/*
 * SoftViz3d Sonar plugin
 * Copyright (C) 2013 Stefan Rinderle
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
package de.rinderle.softviz3d.layout.format;

import de.rinderle.softviz3d.domain.LayoutViewType;
import de.rinderle.softviz3d.domain.MinMaxValue;
import de.rinderle.softviz3d.domain.graph.ResultPlatform;

public interface LayerFormatter {
  void format(ResultPlatform graph, Integer depth, LayoutViewType viewType);

  double calcBuildingHeight(Double value, MinMaxValue minMaxMetricHeight);

  double calcSideLength(Double value, MinMaxValue minMaxMetricFootprint);

  double calcEdgeRadius(int counter, MinMaxValue minMaxEdgeCounter);
}
