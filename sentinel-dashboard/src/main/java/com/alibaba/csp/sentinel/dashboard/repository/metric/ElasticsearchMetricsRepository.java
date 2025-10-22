package com.alibaba.csp.sentinel.dashboard.repository.metric;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *
 *
 * @author ronshi
 * @date 2025/10/21 19:29
 */
@Primary
@Repository("elasticsearchMetricsRepository")
public class ElasticsearchMetricsRepository implements MetricsRepository<MetricEntity>{
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;


    @Override
    public void save(MetricEntity metric) {
        if (metric != null) {
            // 生成唯一ID，避免重复存储
            if (metric.getId() == null) {
                metric.setId(UUID.randomUUID().toString());
            }
            elasticsearchRestTemplate.save(metric);
        }
    }

    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        if (metrics != null) {
            List<MetricEntity> metricsToSave = new ArrayList<>();
            for (MetricEntity metric : metrics) {
                if (metric.getId() == null) {
                    metric.setId(UUID.randomUUID().toString());
                }
                metricsToSave.add(metric);
            }
            // 批量保存，提升性能
            elasticsearchRestTemplate.save(metricsToSave);
        }
    }

    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {
        // 构建查询条件：应用名、资源名且在时间范围内
        Criteria criteria = new Criteria("app").is(app)
            .and("resource").is(resource)
            .and("timestamp").between(new Date(startTime), new Date(endTime));
        Query query = new CriteriaQuery(criteria);
        query.addSort(Sort.by(Sort.Direction.ASC, "timestamp"));

        SearchHits<MetricEntity> searchHits = elasticsearchRestTemplate.search(query, MetricEntity.class);
        return searchHits.getSearchHits().stream()
            .map(hit -> hit.getContent())
            .collect(Collectors.toList());
    }

    @Override
    public List<String> listResourcesOfApp(String app) {
        // 查询指定应用下的所有唯一资源名
        Criteria criteria = new Criteria("app").is(app);
        Query query = new CriteriaQuery(criteria);

        SearchHits<MetricEntity> searchHits = elasticsearchRestTemplate.search(query, MetricEntity.class);
        return searchHits.getSearchHits().stream()
            .map(hit -> hit.getContent().getResource())
            .distinct()
            .collect(Collectors.toList());
    }
}
