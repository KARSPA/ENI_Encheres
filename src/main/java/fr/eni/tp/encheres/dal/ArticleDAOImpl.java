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
import fr.eni.tp.encheres.bo.Auction;
import fr.eni.tp.encheres.bo.Category;
import fr.eni.tp.encheres.bo.User;

@Repository
public class ArticleDAOImpl implements ArticleDAO{
	
	private static final String FIND_BY_ID = "SELECT no_article, nom_article, description, date_debut_encheres, date_fin_encheres, prix_initial, prix_vente, no_utilisateur, no_categorie FROM ARTICLES_VENDUS WHERE no_article = :articleId";
	private static final String FIND_ALL = "SELECT no_article, nom_article, description, date_debut_encheres, date_fin_encheres, prix_initial, prix_vente, no_utilisateur, no_categorie FROM ARTICLES_VENDUS";
	private static final String INSERT = "INSERT INTO ARTICLES_VENDUS (nom_article, description, date_debut_encheres, date_fin_encheres, prix_initial, prix_vente, no_utilisateur,no_categorie) VALUES (:name, :description, :startDate, :endDate, :startPrice, :endPrice, :userId, :categoryId)";
	private static final String DELETE = "DELETE FROM ARTICLES_VENDUS WHERE no_article = :articleId";
	
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	public ArticleDAOImpl(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Article read(long articleId) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("articleId", articleId);
		
		return jdbcTemplate.queryForObject(FIND_BY_ID, mapSqlParameterSource, new ArticleRowMapper());
	}

	@Override
	public List<Article> findAll() {
		return jdbcTemplate.query(FIND_ALL, new ArticleRowMapper());
	}

	@Override
	public List<Article> findByCategory(long categoryId) {
		return null;
	}

	@Override
	public List<Article> findByName(String name) {
		// TODO Auto-generated method stub
		return null;
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
		mapSqlParameterSource.addValue("endPrice", article.getFinalPrice());
		mapSqlParameterSource.addValue("userId", article.getSeller().getUserId());
		mapSqlParameterSource.addValue("categoryId", article.getCategory().getCategoryId());
		
		jdbcTemplate.update(INSERT, mapSqlParameterSource, keyHolder);
		
		if (keyHolder != null && keyHolder.getKey() != null) {
			article.setArticleId(keyHolder.getKey().longValue());
		}
		
	}

	@Override
	public void delete(long articleId) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("articleId", articleId);
		
		jdbcTemplate.update(DELETE, mapSqlParameterSource);
	}

}

class ArticleRowMapper implements RowMapper<Article> {

	@Override
	public Article mapRow(ResultSet rs, int rowNum) throws SQLException {
		Article article = new Article();
		article.setArticleId(rs.getLong("no_article"));
		article.setArticleName(rs.getString("nom_article"));
		article.setDescription(rs.getString("description"));
		article.setAuctionStartDate(rs.getTimestamp("date_debut_encheres").toLocalDateTime());
		article.setAuctionEndDate(rs.getTimestamp("date_fin_encheres").toLocalDateTime());
		article.setBeginningPrice(rs.getInt("prix_initial"));
		article.setFinalPrice(rs.getInt("prix_vente"));
		
		User seller = new User();
		seller.setUserId(rs.getLong("no_utilisateur"));
		article.setSeller(seller);
		
		Category category = new Category();
		category.setCategoryId(rs.getLong("no_categorie"));
		article.setCategory(category);
		
		return article;
	}
}
