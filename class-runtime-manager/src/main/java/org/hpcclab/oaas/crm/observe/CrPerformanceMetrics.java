package org.hpcclab.oaas.crm.observe;

import lombok.Builder;
import org.eclipse.collections.api.factory.primitive.LongDoubleMaps;
import org.hpcclab.oaas.crm.OprcComponent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public record CrPerformanceMetrics(
  Map<OprcComponent, SvcPerformanceMetrics> coreMetrics,
  Map<String, SvcPerformanceMetrics> fnMetrics) {

  public static Double harmonicMean(List<CrPerformanceMetrics.DataPoint> points) {
    var nonZeroNumbers = points.stream()
      .mapToDouble(DataPoint::value)
      .filter(v -> v!=0)
      .toArray();
    if (nonZeroNumbers.length==0) {
      return 0d; // Handle division by zero
    }
    // Calculate harmonic mean
    return nonZeroNumbers.length / Arrays.stream(nonZeroNumbers)
      .map(num -> 1 / num)
      .sum();
  }

  public static Double mean(List<CrPerformanceMetrics.DataPoint> points) {
    return points.stream()
      .mapToDouble(DataPoint::value)
      .average()
      .orElse(0);
  }

  public static List<DataPoint> addingMerge(List<DataPoint> list1, List<DataPoint> list2) {
    var map = LongDoubleMaps.mutable.empty();
    // Add values from list1 to the map
    for (DataPoint dp : list1) {
      map.addToValue(dp.timestamp(), dp.value());
    }

    // Add values from list2 to the map
    for (DataPoint dp : list2) {
      map.addToValue(dp.timestamp(), dp.value());
    }

    return map.keyValuesView()
      .collect(p -> new DataPoint(p.getOne(), p.getTwo()))
      .toSortedList();
  }

  @Builder(toBuilder = true)
  public record SvcPerformanceMetrics(
    List<DataPoint> cpu,
    List<DataPoint> mem,
    List<DataPoint> rps,
    List<DataPoint> msLatency
  ) {
  }

  public record DataPoint(long timestamp, double value) implements Comparable<DataPoint> {
    public int compareTo(DataPoint other) {
      // Compare timestamps
      if (this.timestamp() < other.timestamp()) {
        return -1;
      } else if (this.timestamp() > other.timestamp()) {
        return 1;
      } else {
        // If timestamps are equal, compare values
        return Double.compare(this.value(), other.value());
      }
    }

    @Override
    public String toString() {
      return "[" + timestamp() + ", " + value() + "]";
    }
  }

}
