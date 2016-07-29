package com.lonelyplanet.prometheus

import io.prometheus.client.{Collector, CollectorRegistry}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}
import com.lonelyplanet.prometheus.Utils._

import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

class PrometheusLatencyRecorderSpec extends FlatSpec with Matchers with MockFactory {

  "PrometheusLatencyRecorder" should "register a histogram and record request latencies" in {
    val registry = new CollectorRegistry()
    val randomMetricName = generateRandomString
    val randomMetricHelp = generateRandomString
    val randomLabelName = generateRandomString
    val randomEndpointName = generateRandomString
    val randomLatency = Math.abs(Random.nextInt(10000))

    // our random value will end up in the second bucket
    val buckets = List((randomLatency - 1).toDouble, (randomLatency + 1).toDouble)

    val recorder = new PrometheusLatencyRecorder(
      randomMetricName,
      randomMetricHelp,
      buckets,
      randomLabelName,
      registry,
      duration.MILLISECONDS
    )

    recorder.recordRequestLatency(randomEndpointName, FiniteDuration(randomLatency, duration.MILLISECONDS))

    val first = getBucketValue(registry, randomMetricName, List(randomLabelName), List(randomEndpointName), buckets.head)
    val second = getBucketValue(registry, randomMetricName, List(randomLabelName), List(randomEndpointName), buckets.last)
    val positiveInf = getBucketValue(registry, randomMetricName, List(randomLabelName), List(randomEndpointName), Double.PositiveInfinity)

    first shouldBe 0
    second shouldBe 1
    positiveInf shouldBe 1
  }

  private def getBucketValue(registry: CollectorRegistry, metricName: String, labelNames: List[String], labelValues: List[String], bucket: Double) = {
    val name = metricName + "_bucket"

    // 'le' should be the first label in the list
    val allLabelNames = (Array("le") ++ labelNames).reverse
    val allLabelValues = (Array(Collector.doubleToGoString(bucket)) ++ labelValues).reverse
    registry.getSampleValue(name, allLabelNames, allLabelValues).intValue()
  }

}
