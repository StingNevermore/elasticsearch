---
navigation_title: "Bucket count K-S test"
mapped_pages:
  - https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-count-ks-test-aggregation.html
---

# Bucket count K-S test correlation aggregation [search-aggregations-bucket-count-ks-test-aggregation]


A sibling pipeline aggregation which executes a two sample Kolmogorov–Smirnov test (referred to as a "K-S test" from now on) against a provided distribution, and the distribution implied by the documents counts in the configured sibling aggregation. Specifically, for some metric, assuming that the percentile intervals of the metric are known beforehand or have been computed by an aggregation, then one would use range aggregation for the sibling to compute the p-value of the distribution difference between the metric and the restriction of that metric to a subset of the documents. A natural use case is if the sibling aggregation range aggregation nested in a terms aggregation, in which case one compares the overall distribution of metric to its restriction to each term.

## Parameters [bucket-count-ks-test-agg-syntax]

`buckets_path`
:   (Required, string) Path to the buckets that contain one set of values to correlate. Must be a `_count` path For syntax, see [`buckets_path` Syntax](/reference/aggregations/pipeline.md#buckets-path-syntax).

`alternative`
:   (Optional, list) A list of string values indicating which K-S test alternative to calculate. The valid values are: "greater", "less", "two_sided". This parameter is key for determining the K-S statistic used when calculating the K-S test. Default value is all possible alternative hypotheses.

`fractions`
:   (Optional, list) A list of doubles indicating the distribution of the samples with which to compare to the `buckets_path` results. In typical usage this is the overall proportion of documents in each bucket, which is compared with the actual document proportions in each bucket from the sibling aggregation counts. The default is to assume that overall documents are uniformly distributed on these buckets, which they would be if one used equal percentiles of a metric to define the bucket end points.

`sampling_method`
:   (Optional, string) Indicates the sampling methodology when calculating the K-S test. Note, this is sampling of the returned values. This determines the cumulative distribution function (CDF) points used comparing the two samples. Default is `upper_tail`, which emphasizes the upper end of the CDF points. Valid options are: `upper_tail`, `uniform`, and `lower_tail`.


## Syntax [_syntax_7]

A `bucket_count_ks_test` aggregation looks like this in isolation:

```js
{
  "bucket_count_ks_test": {
    "buckets_path": "range_values>_count", <1>
    "alternative": ["less", "greater", "two_sided"], <2>
    "sampling_method": "upper_tail" <3>
  }
}
```

1. The buckets containing the values to test against.
2. The alternatives to calculate.
3. The sampling method for the K-S statistic.



## Example [bucket-count-ks-test-agg-example]

The following snippet runs the `bucket_count_ks_test` on the individual terms in the field `version` against a uniform distribution. The uniform distribution reflects the `latency` percentile buckets. Not shown is the pre-calculation of the `latency` indicator values, which was done utilizing the [percentiles](/reference/aggregations/search-aggregations-metrics-percentile-aggregation.md) aggregation.

This example is only using the deciles of `latency`.

```console
POST correlate_latency/_search?size=0&filter_path=aggregations
{
  "aggs": {
    "buckets": {
      "terms": { <1>
        "field": "version",
        "size": 2
      },
      "aggs": {
        "latency_ranges": {
          "range": { <2>
            "field": "latency",
            "ranges": [
              { "to": 0 },
              { "from": 0, "to": 105 },
              { "from": 105, "to": 225 },
              { "from": 225, "to": 445 },
              { "from": 445, "to": 665 },
              { "from": 665, "to": 885 },
              { "from": 885, "to": 1115 },
              { "from": 1115, "to": 1335 },
              { "from": 1335, "to": 1555 },
              { "from": 1555, "to": 1775 },
              { "from": 1775 }
            ]
          }
        },
        "ks_test": { <3>
          "bucket_count_ks_test": {
            "buckets_path": "latency_ranges>_count",
            "alternative": ["less", "greater", "two_sided"]
          }
        }
      }
    }
  }
}
```

1. The term buckets containing a range aggregation and the bucket correlation aggregation. Both are utilized to calculate the correlation of the term values with the latency.
2. The range aggregation on the latency field. The ranges were created referencing the percentiles of the latency field.
3. The bucket count K-S test aggregation that tests if the bucket counts comes from the same distribution as `fractions`; where `fractions` is a uniform distribution.


And the following may be the response:

```console-result
{
  "aggregations" : {
    "buckets" : {
      "doc_count_error_upper_bound" : 0,
      "sum_other_doc_count" : 0,
      "buckets" : [
        {
          "key" : "1.0",
          "doc_count" : 100,
          "latency_ranges" : {
            "buckets" : [
              {
                "key" : "*-0.0",
                "to" : 0.0,
                "doc_count" : 0
              },
              {
                "key" : "0.0-105.0",
                "from" : 0.0,
                "to" : 105.0,
                "doc_count" : 1
              },
              {
                "key" : "105.0-225.0",
                "from" : 105.0,
                "to" : 225.0,
                "doc_count" : 9
              },
              {
                "key" : "225.0-445.0",
                "from" : 225.0,
                "to" : 445.0,
                "doc_count" : 0
              },
              {
                "key" : "445.0-665.0",
                "from" : 445.0,
                "to" : 665.0,
                "doc_count" : 0
              },
              {
                "key" : "665.0-885.0",
                "from" : 665.0,
                "to" : 885.0,
                "doc_count" : 0
              },
              {
                "key" : "885.0-1115.0",
                "from" : 885.0,
                "to" : 1115.0,
                "doc_count" : 10
              },
              {
                "key" : "1115.0-1335.0",
                "from" : 1115.0,
                "to" : 1335.0,
                "doc_count" : 20
              },
              {
                "key" : "1335.0-1555.0",
                "from" : 1335.0,
                "to" : 1555.0,
                "doc_count" : 20
              },
              {
                "key" : "1555.0-1775.0",
                "from" : 1555.0,
                "to" : 1775.0,
                "doc_count" : 20
              },
              {
                "key" : "1775.0-*",
                "from" : 1775.0,
                "doc_count" : 20
              }
            ]
          },
          "ks_test" : {
            "less" : 2.248673241788478E-4,
            "greater" : 1.0,
            "two_sided" : 5.791639181800257E-4
          }
        },
        {
          "key" : "2.0",
          "doc_count" : 100,
          "latency_ranges" : {
            "buckets" : [
              {
                "key" : "*-0.0",
                "to" : 0.0,
                "doc_count" : 0
              },
              {
                "key" : "0.0-105.0",
                "from" : 0.0,
                "to" : 105.0,
                "doc_count" : 19
              },
              {
                "key" : "105.0-225.0",
                "from" : 105.0,
                "to" : 225.0,
                "doc_count" : 11
              },
              {
                "key" : "225.0-445.0",
                "from" : 225.0,
                "to" : 445.0,
                "doc_count" : 20
              },
              {
                "key" : "445.0-665.0",
                "from" : 445.0,
                "to" : 665.0,
                "doc_count" : 20
              },
              {
                "key" : "665.0-885.0",
                "from" : 665.0,
                "to" : 885.0,
                "doc_count" : 20
              },
              {
                "key" : "885.0-1115.0",
                "from" : 885.0,
                "to" : 1115.0,
                "doc_count" : 10
              },
              {
                "key" : "1115.0-1335.0",
                "from" : 1115.0,
                "to" : 1335.0,
                "doc_count" : 0
              },
              {
                "key" : "1335.0-1555.0",
                "from" : 1335.0,
                "to" : 1555.0,
                "doc_count" : 0
              },
              {
                "key" : "1555.0-1775.0",
                "from" : 1555.0,
                "to" : 1775.0,
                "doc_count" : 0
              },
              {
                "key" : "1775.0-*",
                "from" : 1775.0,
                "doc_count" : 0
              }
            ]
          },
          "ks_test" : {
            "less" : 0.9642895789647244,
            "greater" : 4.58718174664754E-9,
            "two_sided" : 5.916656831139733E-9
          }
        }
      ]
    }
  }
}
```


