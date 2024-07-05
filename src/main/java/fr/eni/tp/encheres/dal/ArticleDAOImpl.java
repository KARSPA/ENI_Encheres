package fr.eni.tp.encheres.dal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import fr.eni.tp.encheres.bo.Article;
import fr.eni.tp.encheres.bo.Category;
import fr.eni.tp.encheres.bo.User;

@Repository
public class ArticleDAOImpl implements ArticleDAO{
	
	private static final String FIND_BY_ID = "SELECT no_article, nom_article, description, date_debut_encheres, date_fin_encheres, prix_initial, prix_vente, no_utilisateur, no_categorie FROM ARTICLES_VENDUS WHERE no_article = :articleId";
	private static final String FIND_ALL = "SELECT no_article, nom_article, description, date_debut_encheres, date_fin_encheres, prix_initial, prix_vente, no_utilisateur, no_categorie FROM ARTICLES_VENDUS";
	private static final String FIND_BY_CATEGORY = "SELECT no_article, nom_article, description, date_debut_encheres, date_fin_encheres, prix_initial, prix_vente, no_utilisateur, no_categorie FROM ARTICLES_VENDUS WHERE no_categorie = :categoryId";
	private static final String FIND_BY_NAME = "SELECT no_article, nom_article, description, date_debut_encheres, date_fin_encheres, prix_initial, prix_vente, no_utilisateur, no_categorie FROM ARTICLES_VENDUS WHERE nom_article LIKE :name";
	private static final String FIND_BY_CATEGORY_AND_NAME = "SELECT no_article, nom_article, description, date_debut_encheres, date_fin_encheres, prix_initial, prix_vente, no_utilisateur, no_categorie FROM ARTICLES_VENDUS WHERE no_categorie = :categoryId AND nom_article LIKE :name";
	private static final String INSERT = "INSERT INTO ARTICLES_VENDUS (nom_article, description, date_debut_encheres, date_fin_encheres, prix_initial, prix_vente, no_utilisateur,no_categorie) VALUES (:name, :description, :startDate, :endDate, :startPrice, :endPrice, :userId, :categoryId)";
	private static final String DELETE = "DELETE FROM ARTICLES_VENDUS WHERE no_article = :articleId";
	
	private static final String UPDATE_SELL_PRICE_AND_BUYER = "UPDATE ARTICLES_VENDUS SET prix_vente = :newBid, no_acheteur = :userId WHERE no_article = :articleId";
	private static final String UPDATE = "UPDATE ARTICLES_VENDUS SET nom_article = :name, description =:description, date_debut_encheres =:startDate, date_fin_encheres=:endDate, prix_initial=:startPrice, prix_vente=:endPrice, no_categorie=:categoryId WHERE no_article = :articleId";
	
	private static final String SCHEDULED_COUNT = "SELECT count(*) FROM ARTICLES_VENDUS WHERE no_article > :idMin";
	
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	public ArticleDAOImpl(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Article read(int articleId) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("articleId", articleId);
		
		return jdbcTemplate.queryForObject(FIND_BY_ID, mapSqlParameterSource, new ArticleRowMapper());
	}

	@Override
	public List<Article> findAll() {
		return jdbcTemplate.query(FIND_ALL, new ArticleRowMapper());
	}

	@Override
	public List<Article> findByCategory(int categoryId) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("categoryId", categoryId);
		
		return jdbcTemplate.query(FIND_BY_CATEGORY, mapSqlParameterSource, new ArticleRowMapper());
	}

	@Override
	public List<Article> findByCategoryAndName(int categoryId, String name) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("categoryId", categoryId);
		mapSqlParameterSource.addValue("name", name);
		
		return jdbcTemplate.query(FIND_BY_CATEGORY_AND_NAME, mapSqlParameterSource, new ArticleRowMapper());
	}

	@Override
	public List<Article> findByName(String name) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("name", "%"+name+"%");

		return jdbcTemplate.query(FIND_BY_NAME, mapSqlParameterSource, new ArticleRowMapper());
	}

	@Override
	public void create(Article article) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("name", article.getArticleName());
		mapSqlParameterSource.addValue("description", article.getDescription());
		mapSqlParameterSource.addValue("startDate", article.getAuctionStartDate());
		mapSqlParameterSource.addValue("endDate", article.getAuctionEndDate());
		mapSqlParameterSource.addValue("startPrice", article.getBeginningPrice());
		mapSqlParameterSource.addValue("endPrice", article.getCurrentPrice());
		mapSqlParameterSource.addValue("userId", article.getSeller().getUserId());
		mapSqlParameterSource.addValue("categoryId", article.getCategory().getCategoryId());
		
		jdbcTemplate.update(INSERT, mapSqlParameterSource, keyHolder);
		
		if (keyHolder != null && keyHolder.getKey() != null) {
			article.setArticleId(keyHolder.getKey().intValue());
		}
		
	}
	
	@Override
	public void updateSellPriceAndBuyer(int articleId, int newPrice, int userId) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("articleId", articleId);
		mapSqlParameterSource.addValue("userId", userId);
		mapSqlParameterSource.addValue("newBid", newPrice);
		
		jdbcTemplate.update(UPDATE_SELL_PRICE_AND_BUYER, mapSqlParameterSource);
	}
	
	@Override
	public void updateArticle(Article article) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("articleId", article.getArticleId());
		mapSqlParameterSource.addValue("name", article.getArticleName());
		mapSqlParameterSource.addValue("description", article.getDescription());
		mapSqlParameterSource.addValue("startDate", article.getAuctionStartDate());
		mapSqlParameterSource.addValue("endDate", article.getAuctionEndDate());
		mapSqlParameterSource.addValue("startPrice", article.getBeginningPrice());
		mapSqlParameterSource.addValue("endPrice", article.getCurrentPrice());
		mapSqlParameterSource.addValue("userId", article.getSeller().getUserId());
		mapSqlParameterSource.addValue("categoryId", article.getCategory().getCategoryId());
		
		jdbcTemplate.update(UPDATE, mapSqlParameterSource);
	}

	@Override
	public void delete(int articleId) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("articleId", articleId);
		
		jdbcTemplate.update(DELETE, mapSqlParameterSource);
	}
	
	@Override
	public int countArticles() {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("idMin", 0);
		
		return jdbcTemplate.queryForObject(SCHEDULED_COUNT,mapSqlParameterSource, Integer.class);
	}

}

class ArticleRowMapper implements RowMapper<Article> {

	@Override
	public Article mapRow(ResultSet rs, int rowNum) throws SQLException {
		Article article = new Article();
		article.setArticleId(rs.getInt("no_article"));
		article.setArticleName(rs.getString("nom_article"));
		article.setDescription(rs.getString("description"));
		article.setAuctionStartDate(rs.getTimestamp("date_debut_encheres").toLocalDateTime());
		article.setAuctionEndDate(rs.getTimestamp("date_fin_encheres").toLocalDateTime());
		
		article.setBeginningPrice(rs.getInt("prix_initial"));
		article.setCurrentPrice(rs.getInt("prix_vente"));
		
		User seller = new User();
		seller.setUserId(rs.getInt("no_utilisateur"));
		article.setSeller(seller);
		
		Category category = new Category();
		category.setCategoryId(rs.getInt("no_categorie"));
		article.setCategory(category);
		
		return article;
	}
}
