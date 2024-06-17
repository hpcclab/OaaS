package org.hpcclab.oaas.crm.observe;

import lombok.Builder;
import org.eclipse.collections.api.factory.primitive.LongDoubleMaps;
import org.hpcclab.oaas.crm.CrComponent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record CrPerformanceMetrics(
  Map<CrComponent, SvcPerformanceMetrics> coreMetrics,
  Map<String, SvcPerformanceMetrics> fnMetrics) {

  public static Double harmonicMean(List<CrPerformanceMetrics.DataPoint> points) {
    var nonZeroNumbers = points.stream()
      .mapToDouble(DataPoint::value)
      .filter(v -> v!=0 && !Double.isNaN(v))
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
      .filter(v -> !Double.isNaN(v))
      .average()
      .orElse(0);
  }

  public static Double divideThenMean(List<CrPerformanceMetrics.DataPoint> dp1List,
                                      List<CrPerformanceMetrics.DataPoint> dp2List) {
    double sum = 0;
    double count = 0;
    Map<Long, Double> dp2Map = dp2List.stream().collect(Collectors.toMap(DataPoint::timestamp, DataPoint::value));
    for (DataPoint dp1 : dp1List) {
      double dp2Value = dp2Map.getOrDefault(dp1.timestamp(), 0d);
      if (dp2Value==0) continue;
      sum += dp1.value / dp2Value;
      count++;
    }
    if (count==0) return 0d;
    return sum / count;
  }

  public static List<DataPoint> addingMerge(List<DataPoint> list1, List<DataPoint> list2) {
    var map = LongDoubleMaps.mutable.empty();
    // Add values from list1 to the map
    for (DataPoint dp : list1) {
      if (!Double.isNaN(dp.value))
        map.addToValue(dp.timestamp(), dp.value());
    }

    // Add values from list2 to the map
    for (DataPoint dp : list2) {
      if (!Double.isNaN(dp.value))
        map.addToValue(dp.timestamp(), dp.value());
    }

    return map.keyValuesView()
      .collect(p -> new DataPoint(p.getOne(), p.getTwo()))
      .toSortedList();
  }

  public static List<DataPoint> filterByTime(List<DataPoint> dps, long ts) {
    return dps.stream().filter(d -> d.timestamp >= ts).toList();
  }

  @Builder(toBuilder = true)
  public record SvcPerformanceMetrics(
    List<DataPoint> cpu,
    List<DataPoint> mem,
    List<DataPoint> rps,
    List<DataPoint> msLatency) {

    public SvcPerformanceMetrics filterByStableTime(long stableTime) {
      if (stableTime < 0) return this;
      return new SvcPerformanceMetrics(
        filterByTime(cpu, stableTime),
        filterByTime(mem, stableTime),
        filterByTime(rps, stableTime),
        filterByTime(msLatency, stableTime)
      );
    }

    public SvcPerformanceMetrics syncTimeOnCpuMemRps() {
      long min = Stream.of(cpu, mem, rps)
        .mapToLong(dataPoints -> dataPoints.stream()
          .mapToLong(DataPoint::timestamp)
          .min().orElse(0))
        .max()
        .orElse(0L);
      long max = Stream.of(cpu, mem, rps)
        .mapToLong(dataPoints -> dataPoints.stream()
          .mapToLong(DataPoint::timestamp)
          .max().orElse(0))
        .min()
        .orElse(0L);
      var newCpu = cpu.stream()
        .filter(d -> d.timestamp >= min && d.timestamp <= max)
        .toList();
      var newMem = mem.stream()
        .filter(d -> d.timestamp >= min && d.timestamp <= max)
        .toList();
      var newRps = rps.stream()
        .filter(d -> d.timestamp >= min && d.timestamp <= max)
        .toList();
      return toBuilder()
        .cpu(newCpu)
        .mem(newMem)
        .rps(newRps)
        .build();
    }
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
