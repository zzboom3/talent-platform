package com.talent.platform.repository;

import com.talent.platform.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByCategoryOrderByPublishTimeDesc(News.Category category);
    List<News> findAllByOrderByPublishTimeDesc();
    boolean existsByTitle(String title);
    long countByCategory(News.Category category);

    @Query("SELECT COUNT(n) FROM News n WHERE n.sourceType = :sourceType")
    long countBySourceType(@Param("sourceType") News.SourceType sourceType);

    @Query("SELECT COUNT(n) FROM News n WHERE n.sourceType = :sourceType AND n.category = :category")
    long countBySourceTypeAndCategory(@Param("sourceType") News.SourceType sourceType, @Param("category") News.Category category);
    @Query(value = """
            SELECT COUNT(*) FROM news
            WHERE source_type = :sourceType
               OR source_site IN (:sourceSites)
            """, nativeQuery = true)
    long countCrawledNews(@Param("sourceType") String sourceType,
                          @Param("sourceSites") List<String> sourceSites);

    @Query(value = """
            SELECT COUNT(*) FROM news
            WHERE category = :category
              AND (
                    source_type = :sourceType
                    OR source_site IN (:sourceSites)
              )
            """, nativeQuery = true)
    long countCrawledNewsByCategory(@Param("category") String category,
                                    @Param("sourceType") String sourceType,
                                    @Param("sourceSites") List<String> sourceSites);
    long countByCategoryAndPublishTimeAfter(News.Category category, java.time.LocalDateTime publishTime);
    List<News> findByReviewStatusOrderByPublishTimeDesc(News.ReviewStatus reviewStatus);
    List<News> findByCategoryAndReviewStatusOrderByPublishTimeDesc(News.Category category, News.ReviewStatus reviewStatus);
    List<News> findByReviewStatusAndCategoryInOrderByPublishTimeDesc(News.ReviewStatus reviewStatus, List<News.Category> categories);
    @Query("""
            SELECT n FROM News n
            WHERE (:category IS NULL OR n.category = :category)
              AND (:reviewStatus IS NULL OR n.reviewStatus = :reviewStatus)
              AND (:sourceType IS NULL OR n.sourceType = :sourceType)
              AND (
                    :keyword IS NULL
                    OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(n.sourceSite, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(n.author, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY n.publishTime DESC
            """)
    List<News> searchAdminNews(@Param("category") News.Category category,
                               @Param("reviewStatus") News.ReviewStatus reviewStatus,
                               @Param("sourceType") News.SourceType sourceType,
                               @Param("keyword") String keyword);

    @Query("SELECT n FROM News n WHERE (n.reviewStatus = :reviewStatus OR n.reviewStatus IS NULL) ORDER BY n.publishTime DESC")
    List<News> findPublishedOrderByPublishTimeDesc(@Param("reviewStatus") News.ReviewStatus reviewStatus);

    @Query("SELECT n FROM News n WHERE n.category = :category AND (n.reviewStatus = :reviewStatus OR n.reviewStatus IS NULL) ORDER BY n.publishTime DESC")
    List<News> findPublishedByCategoryOrderByPublishTimeDesc(@Param("category") News.Category category,
                                                             @Param("reviewStatus") News.ReviewStatus reviewStatus);

    @Query("""
            SELECT n FROM News n
            WHERE (n.reviewStatus = :reviewStatus OR n.reviewStatus IS NULL)
              AND COALESCE(n.sourceSite, '') NOT IN :excludedSourceSites
            ORDER BY n.publishTime DESC
            """)
    List<News> findPublicPublishedExcludingSourcesOrderByPublishTimeDesc(@Param("reviewStatus") News.ReviewStatus reviewStatus,
                                                                         @Param("excludedSourceSites") List<String> excludedSourceSites);

    @Query("""
            SELECT n FROM News n
            WHERE n.category = :category
              AND (n.reviewStatus = :reviewStatus OR n.reviewStatus IS NULL)
              AND COALESCE(n.sourceSite, '') NOT IN :excludedSourceSites
            ORDER BY n.publishTime DESC
            """)
    List<News> findPublicPublishedByCategoryExcludingSourcesOrderByPublishTimeDesc(@Param("category") News.Category category,
                                                                                    @Param("reviewStatus") News.ReviewStatus reviewStatus,
                                                                                    @Param("excludedSourceSites") List<String> excludedSourceSites);
}
