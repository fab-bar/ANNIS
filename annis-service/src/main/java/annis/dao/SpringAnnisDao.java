/*
 * Copyright 2009-2011 Collaborative Research Centre SFB 632
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package annis.dao;

import annis.examplequeries.ExampleQuery;
import annis.exceptions.AnnisException;
import annis.model.Annotation;
import annis.ql.node.Start;
import annis.ql.parser.AnnisParser;
import annis.ql.parser.QueryAnalysis;
import annis.ql.parser.QueryData;
import annis.resolver.ResolverEntry;
import annis.resolver.SingleResolverRequest;
import annis.service.objects.AnnisAttribute;
import annis.service.objects.AnnisBinaryMetaData;
import annis.service.objects.AnnisCorpus;
import annis.service.objects.Match;
import annis.service.objects.MatchAndDocumentCount;
import annis.sqlgen.AnnotateSqlGenerator;
import annis.sqlgen.ByteHelper;
import annis.sqlgen.CountMatchesAndDocumentsSqlGenerator;
import annis.sqlgen.CountSqlGenerator;
import annis.sqlgen.FindSqlGenerator;
import annis.sqlgen.ListDocumentsSqlHelper;
import annis.sqlgen.ListAnnotationsSqlHelper;
import annis.sqlgen.ListCorpusAnnotationsSqlHelper;
import annis.sqlgen.ListDocumentsAnnotationsSqlHelper;
import annis.sqlgen.ListCorpusSqlHelper;
import annis.sqlgen.ListExampleQueriesHelper;
import annis.sqlgen.MatrixSqlGenerator;
import annis.sqlgen.MetaByteHelper;
import annis.sqlgen.SaltAnnotateExtractor;
import annis.sqlgen.SqlGenerator;
import annis.utils.Utils;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.SaltProject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.util.ListIterator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.apache.commons.io.input.BoundedInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.simple.ParameterizedSingleColumnRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;
import org.springframework.transaction.annotation.Transactional;

// FIXME: test and refactor timeout and transaction management
public class SpringAnnisDao extends SimpleJdbcDaoSupport implements AnnisDao,
  SqlSessionModifier
{
  
  // SQL generators for the different query functions
  private FindSqlGenerator findSqlGenerator;

  private CountMatchesAndDocumentsSqlGenerator countMatchesAndDocumentsSqlGenerator;

  private CountSqlGenerator countSqlGenerator;

  private AnnotateSqlGenerator<SaltProject> annotateSqlGenerator;

  private SaltAnnotateExtractor saltAnnotateExtractor;

  private MatrixSqlGenerator matrixSqlGenerator;

  // generated sql for example queries and fetches the result
  private ListExampleQueriesHelper listExampleQueriesHelper;

  private AnnotateSqlGenerator<SaltProject> graphSqlGenerator;

  private String externalFilesPath;
  // configuration

  private int timeout;
  // fn: corpus id -> corpus name

  private Map<Long, String> corpusNamesById;

  @Override
  @Transactional
  public SaltProject graph(QueryData data)
  {
    return executeQueryFunction(data, graphSqlGenerator, saltAnnotateExtractor);
  }

  /**
   * @return the graphSqlGenerator
   */
  public AnnotateSqlGenerator getGraphSqlGenerator()
  {
    return graphSqlGenerator;
  }

  /**
   * @param graphSqlGenerator the graphSqlGenerator to set
   */
  public void setGraphSqlGenerator(AnnotateSqlGenerator graphSqlGenerator)
  {
    this.graphSqlGenerator = graphSqlGenerator;
  }

  /**
   * @return the listDocumentsAnnotationsSqlHelper
   */
  public ListDocumentsAnnotationsSqlHelper getListDocumentsAnnotationsSqlHelper()
  {
    return listDocumentsAnnotationsSqlHelper;
  }

  /**
   * @param listDocumentsAnnotationsSqlHelper the
   * listDocumentsAnnotationsSqlHelper to set
   */
  public void setListDocumentsAnnotationsSqlHelper(
    ListDocumentsAnnotationsSqlHelper listDocumentsAnnotationsSqlHelper)
  {
    this.listDocumentsAnnotationsSqlHelper = listDocumentsAnnotationsSqlHelper;
  }

  @Override
  public List<Annotation> listDocuments(String toplevelCorpusName)
  {
    return (List<Annotation>) getJdbcTemplate().query(
      getListDocumentsSqlHelper().
      createSql(toplevelCorpusName), getListDocumentsSqlHelper());
  }

  /**
   * @return the listDocumentsSqlHelper
   */
  public ListDocumentsSqlHelper getListDocumentsSqlHelper()
  {
    return listDocumentsSqlHelper;
  }

  /**
   * @param listDocumentsSqlHelper the listDocumentsSqlHelper to set
   */
  public void setListDocumentsSqlHelper(
    ListDocumentsSqlHelper listDocumentsSqlHelper)
  {
    this.listDocumentsSqlHelper = listDocumentsSqlHelper;
  }

//	private MatrixSqlGenerator matrixSqlGenerator;
  // SqlGenerator that prepends EXPLAIN to a query
  private static final class ExplainSqlGenerator implements
    SqlGenerator<QueryData, String>
  {

    private final boolean analyze;

    private final SqlGenerator<QueryData, ?> generator;

    private ExplainSqlGenerator(SqlGenerator<QueryData, ?> generator,
      boolean analyze)
    {
      this.generator = generator;
      this.analyze = analyze;
    }

    @Override
    public String toSql(QueryData queryData)
    {
      StringBuilder sb = new StringBuilder();
      sb.append("EXPLAIN ");
      if (analyze)
      {
        sb.append("ANALYZE ");
      }
      sb.append(generator.toSql(queryData));
      return sb.toString();
    }

    @Override
    public String extractData(ResultSet rs) throws SQLException,
      DataAccessException
    {
      StringBuilder sb = new StringBuilder();
      while (rs.next())
      {
        sb.append(rs.getString(1));
        sb.append("\n");
      }
      return sb.toString();
    }

    @Override
    public String toSql(QueryData queryData, String indent)
    {
      // dont indent
      return toSql(queryData);
    }
  }
  private static final Logger log = LoggerFactory.
    getLogger(SpringAnnisDao.class);
  // / old

  private SqlGenerator sqlGenerator;

  private ListCorpusSqlHelper listCorpusSqlHelper;

  private ListAnnotationsSqlHelper listAnnotationsSqlHelper;

  private ListCorpusAnnotationsSqlHelper listCorpusAnnotationsSqlHelper;

  private ListDocumentsAnnotationsSqlHelper listDocumentsAnnotationsSqlHelper;

  private ListDocumentsSqlHelper listDocumentsSqlHelper;
  // / new

  private List<SqlSessionModifier> sqlSessionModifiers;
//  private SqlGenerator findSqlGenerator;

  private ParameterizedSingleColumnRowMapper<String> planRowMapper;

  private ListCorpusByNameDaoHelper listCorpusByNameDaoHelper;

  private AnnotateSqlGenerator graphExtractor;

  private MetaDataFilter metaDataFilter;

  private QueryAnalysis queryAnalysis;

  private AnnisParser aqlParser;

  private HashMap<Long, Properties> corpusConfiguration;

  private ByteHelper byteHelper;

  private MetaByteHelper metaByteHelper;

  public SpringAnnisDao()
  {
    planRowMapper = new ParameterizedSingleColumnRowMapper<String>();
    sqlSessionModifiers = new ArrayList<SqlSessionModifier>();
  }

  public void init()
  {
    parseCorpusConfiguration();
  }

  @Override
  public <T> T executeQueryFunction(QueryData queryData,
    final SqlGenerator<QueryData, T> generator)
  {
    return executeQueryFunction(queryData, generator, generator);
  }

  @Override
  public List<String> mapCorpusIdsToNames(List<Long> ids)
  {
    List<String> names = new ArrayList<String>();
    if (corpusNamesById == null)
    {
      corpusNamesById = new TreeMap<Long, String>();
      List<AnnisCorpus> corpora = listCorpora();
      for (AnnisCorpus corpus : corpora)
      {
        corpusNamesById.put(corpus.getId(), corpus.getName());
      }
    }
    for (Long id : ids)
    {
      names.add(corpusNamesById.get(id));
    }
    return names;
  }

  // query functions
  @Transactional
  @Override
  public <T> T executeQueryFunction(QueryData queryData,
    final SqlGenerator<QueryData, T> generator,
    final ResultSetExtractor<T> extractor)
  {

    JdbcTemplate jdbcTemplate = getJdbcTemplate();

    // FIXME: muss corpusConfiguration an jeden Query angehangen werden?
    // oder nur an annotate-Queries?

    queryData.setCorpusConfiguration(corpusConfiguration);

    // filter by meta data
    queryData.setDocuments(metaDataFilter.getDocumentsForMetadata(queryData));

    // execute session modifiers if any
    for (SqlSessionModifier sqlSessionModifier : sqlSessionModifiers)
    {
      sqlSessionModifier.modifySqlSession(jdbcTemplate, queryData);
    }

    // execute query and return result
    return jdbcTemplate.query(generator.toSql(queryData), extractor);
  }

  @Override
  public void modifySqlSession(JdbcTemplate jdbcTemplate, QueryData queryData)
  {
    if (timeout > 0)
    {
      jdbcTemplate.update("SET statement_timeout TO " + timeout);
    }
  }

  @Override
  public List<ExampleQuery> getExampleQueries(List<Long> corpusIDs)
  {
    return (List<ExampleQuery>) getJdbcTemplate().query(
      listExampleQueriesHelper.createSQLQuery(corpusIDs),
      listExampleQueriesHelper);
  }

  @Transactional(readOnly = true)
  @Override
  public List<Match> find(QueryData queryData)
  {
    return executeQueryFunction(queryData, findSqlGenerator);
  }

  @Transactional(readOnly = true)
  @Override
  public int count(QueryData queryData)
  {
    return executeQueryFunction(queryData, countSqlGenerator);
  }

  @Transactional(readOnly = true)
  @Override
  public MatchAndDocumentCount countMatchesAndDocuments(QueryData queryData)
  {
    return executeQueryFunction(queryData, countMatchesAndDocumentsSqlGenerator);
  }

  @Override
  @Transactional(readOnly = true)
  public SaltProject annotate(QueryData queryData)
  {
    return executeQueryFunction(queryData, annotateSqlGenerator,
      saltAnnotateExtractor);
  }

  @Transactional(readOnly = true)
  @Override
  public List<AnnotatedMatch> matrix(QueryData queryData)
  {
    return executeQueryFunction(queryData, matrixSqlGenerator);
  }

  @Override
  @Transactional(readOnly = true)
  public String explain(SqlGenerator<QueryData, ?> generator,
    QueryData queryData,
    final boolean analyze)
  {
    return executeQueryFunction(queryData, new ExplainSqlGenerator(generator,
      analyze));
  }

  @Override
  public QueryData parseAQL(String aql, List<Long> corpusList)
  {
    // parse the query
    Start statement = aqlParser.parse(aql);
    // analyze it
    return queryAnalysis.analyzeQuery(statement, corpusList);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AnnisCorpus> listCorpora()
  {
    return (List<AnnisCorpus>) getJdbcTemplate().query(
      listCorpusSqlHelper.createSqlQuery(), listCorpusSqlHelper);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AnnisAttribute> listAnnotations(List<Long> corpusList,
    boolean listValues, boolean onlyMostFrequentValues)
  {
    return (List<AnnisAttribute>) getJdbcTemplate().query(
      listAnnotationsSqlHelper.createSqlQuery(corpusList, listValues,
      onlyMostFrequentValues), listAnnotationsSqlHelper);
  }

  @Override
  @Transactional(readOnly = true)
  public SaltProject retrieveAnnotationGraph(String toplevelCorpusName,
    String documentName)
  {
    SaltProject p =
      annotateSqlGenerator.queryAnnotationGraph(getJdbcTemplate(),
      toplevelCorpusName, documentName);
    return p;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Annotation> listCorpusAnnotations(String toplevelCorpusName)
  {
    final String sql = listCorpusAnnotationsSqlHelper.createSqlQuery(
      toplevelCorpusName, toplevelCorpusName, true);
    final List<Annotation> corpusAnnotations =
      (List<Annotation>) getJdbcTemplate().query(sql,
      listCorpusAnnotationsSqlHelper);
    return corpusAnnotations;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Annotation> listDocumentsAnnotations(String toplevelCorpusName,
    boolean listRootCorpus)
  {
    final String sql = listDocumentsAnnotationsSqlHelper.createSqlQuery(
      toplevelCorpusName, listRootCorpus);
    final List<Annotation> docAnnotations =
      (List<Annotation>) getJdbcTemplate().query(sql,
      listDocumentsAnnotationsSqlHelper);
    return docAnnotations;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Annotation> listCorpusAnnotations(String toplevelCorpusName,
    String documentName, boolean exclude)
  {
    String sql = listCorpusAnnotationsSqlHelper.createSqlQuery(
      toplevelCorpusName, documentName, exclude);
    final List<Annotation> cA = (List<Annotation>) getJdbcTemplate().query(sql,
      listCorpusAnnotationsSqlHelper);
    return cA;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Long> mapCorpusNamesToIds(List<String> corpusNames)
  {
    final String sql = listCorpusByNameDaoHelper.createSql(corpusNames);
    final List<Long> result = getJdbcTemplate().query(sql,
      listCorpusByNameDaoHelper);
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ResolverEntry> getResolverEntries(SingleResolverRequest request)
  {
    try
    {
      ResolverDaoHelper helper = new ResolverDaoHelper();
      PreparedStatement stmt = helper.createPreparedStatement(getConnection());
      helper.fillPreparedStatement(request, stmt);
      List<ResolverEntry> result = helper.extractData(stmt.executeQuery());
      return result;
    }
    catch (SQLException ex)
    {
      log.error("Could not get resolver entries from database", ex);
      return new LinkedList<ResolverEntry>();
    }
  }

  @Override
  public Map<String, String> getCorpusConfiguration(String corpusName)
  {
    Map<String, String> result = new TreeMap<String, String>();

    // parse from configuration folder
    if (System.getProperty("annis.home") != null)
    {
      File confFolder = new File(System.getProperty("annis.home")
        + "/conf/corpora");
      if (confFolder.isDirectory())
      {

        File conf = null;
        try
        {
          // try  hash of corpus name first
          conf = new File(confFolder, Utils.calculateSHAHash(corpusName));
        }
        catch (NoSuchAlgorithmException ex)
        {
          log.warn(null, ex);
        }
        catch (UnsupportedEncodingException ex)
        {
          log.warn(null, ex);
        }

        if (conf == null || !conf.isFile())
        {
          // try corpus name next
          conf = new File(confFolder, corpusName + ".properties");
        }
        // parse property file if found
        if (conf.isFile())
        {
          Properties p = new Properties();
          FileInputStream confStream = null;
          try
          {
            confStream = new FileInputStream(conf);
            p.load(confStream);
            for (Map.Entry<Object, Object> e : p.entrySet())
            {
              result.put(e.getKey().toString(), e.getValue().toString());
            }
          }
          catch (IOException ex)
          {
            log.warn("could not load corpus configuration file "
              + conf.getAbsolutePath(), ex);
          }
          finally
          {
            if (confStream != null)
            {
              try
              {
                confStream.close();
              }
              catch (IOException ex)
              {
                log.warn(null, ex);
              }
            }
          }
        }
      } // end if conf is a directory
    } // end if annis.home was set
    return result;
  }

  private void parseCorpusConfiguration()
  {
    corpusConfiguration = new HashMap<Long, Properties>();

    try
    {
      List<AnnisCorpus> corpora = listCorpora();
      for (AnnisCorpus c : corpora)
      {
        // copy properties from map
        Properties p = new Properties();
        Map<String, String> map = getCorpusConfiguration(c.getName());
        for (Map.Entry<String, String> e : map.entrySet())
        {
          p.setProperty(e.getKey(), e.getValue());
        }
        corpusConfiguration.put(c.getId(), p);
      }
    }
    catch (org.springframework.jdbc.CannotGetJdbcConnectionException ex)
    {
      log.warn(
        "No corpus configuration loaded due to missing database connection.");
    }
  }

  @Override
  public boolean checkDatabaseVersion() throws AnnisException
  {
    Connection conn = null;
    try
    {
      conn = getJdbcTemplate().getDataSource().getConnection();
      DatabaseMetaData meta = conn.getMetaData();


      log.debug(
        "database info [major: " + meta.getDatabaseMajorVersion() + " minor: " + meta.
        getDatabaseMinorVersion() + " complete: " + meta.
        getDatabaseProductVersion() + " name: " + meta.getDatabaseProductName() + "]");

      if (!"PostgreSQL".equalsIgnoreCase(meta.getDatabaseProductName()))
      {
        throw new AnnisException("You did provide a database connection to a "
          + "database that is not PostgreSQL. Please note that this will "
          + "not work.");
      }
      if (meta.getDatabaseMajorVersion() < 9
        || (meta.getDatabaseMajorVersion() == 9 && meta.
        getDatabaseMinorVersion() < 1)) // we urge people to use 9.2, but 9.1 should be valid as well
      {
        throw new AnnisException("Wrong PostgreSQL version installed. Please "
          + "install at least PostgreSQL 9.2 (current installed version is "
          + meta.getDatabaseProductVersion() + ")");
      }
    }
    catch (SQLException ex)
    {
      log.error("could not get database version", ex);
    }
    finally
    {
      if (conn != null)
      {
        try
        {
          conn.close();
        }
        catch (SQLException ex)
        {
          log.error(null, ex);
        }
      }
    }
    return false;
  }

  public AnnisParser getAqlParser()
  {
    return aqlParser;
  }

  public void setAqlParser(AnnisParser aqlParser)
  {
    this.aqlParser = aqlParser;
  }

  // /// Getter / Setter
  public SqlGenerator getSqlGenerator()
  {
    return sqlGenerator;
  }

  public void setSqlGenerator(SqlGenerator sqlGenerator)
  {
    this.sqlGenerator = sqlGenerator;
  }

  public ParameterizedSingleColumnRowMapper<String> getPlanRowMapper()
  {
    return planRowMapper;
  }

  public void setPlanRowMapper(
    ParameterizedSingleColumnRowMapper<String> planRowMapper)
  {
    this.planRowMapper = planRowMapper;
  }

  public ListCorpusSqlHelper getListCorpusSqlHelper()
  {
    return listCorpusSqlHelper;
  }

  public void setListCorpusSqlHelper(ListCorpusSqlHelper listCorpusHelper)
  {
    this.listCorpusSqlHelper = listCorpusHelper;
  }

  public ListAnnotationsSqlHelper getListAnnotationsSqlHelper()
  {
    return listAnnotationsSqlHelper;
  }

  public void setListAnnotationsSqlHelper(
    ListAnnotationsSqlHelper listNodeAnnotationsSqlHelper)
  {
    this.listAnnotationsSqlHelper = listNodeAnnotationsSqlHelper;
  }

  public ListCorpusAnnotationsSqlHelper getListCorpusAnnotationsSqlHelper()
  {
    return listCorpusAnnotationsSqlHelper;
  }

  public void setListCorpusAnnotationsSqlHelper(
    ListCorpusAnnotationsSqlHelper listCorpusAnnotationsHelper)
  {
    this.listCorpusAnnotationsSqlHelper = listCorpusAnnotationsHelper;
  }

  public List<SqlSessionModifier> getSqlSessionModifiers()
  {
    return sqlSessionModifiers;
  }

  public void setSqlSessionModifiers(
    List<SqlSessionModifier> sqlSessionModifiers)
  {
    this.sqlSessionModifiers = sqlSessionModifiers;
  }

  public FindSqlGenerator getFindSqlGenerator()
  {
    return findSqlGenerator;
  }

  public void setFindSqlGenerator(FindSqlGenerator findSqlGenerator)
  {
    this.findSqlGenerator = findSqlGenerator;
  }

  public QueryAnalysis getQueryAnalysis()
  {
    return queryAnalysis;
  }

  public void setQueryAnalysis(QueryAnalysis queryAnalysis)
  {
    this.queryAnalysis = queryAnalysis;
  }

  public ListCorpusByNameDaoHelper getListCorpusByNameDaoHelper()
  {
    return listCorpusByNameDaoHelper;
  }

  public void setListCorpusByNameDaoHelper(
    ListCorpusByNameDaoHelper listCorpusByNameDaoHelper)
  {
    this.listCorpusByNameDaoHelper = listCorpusByNameDaoHelper;
  }

  public AnnotateSqlGenerator getGraphExtractor()
  {
    return graphExtractor;
  }

  public void setGraphExtractor(AnnotateSqlGenerator graphExtractor)
  {
    this.graphExtractor = graphExtractor;
  }

  public MetaDataFilter getMetaDataFilter()
  {
    return metaDataFilter;
  }

  public void setMetaDataFilter(MetaDataFilter metaDataFilter)
  {
    this.metaDataFilter = metaDataFilter;
  }

  public CountMatchesAndDocumentsSqlGenerator getCountMatchesAndDocumentsSqlGenerator()
  {
    return countMatchesAndDocumentsSqlGenerator;
  }

  public void setCountMatchesAndDocumentsSqlGenerator(
    CountMatchesAndDocumentsSqlGenerator countSqlGenerator)
  {
    this.countMatchesAndDocumentsSqlGenerator = countSqlGenerator;
  }

  @Override
  public HashMap<Long, Properties> getCorpusConfiguration()
  {
    return corpusConfiguration;
  }

  @Override
  public void setCorpusConfiguration(
    HashMap<Long, Properties> corpusConfiguration)
  {
    this.corpusConfiguration = corpusConfiguration;
  }

  @Override
  public int getTimeout()
  {
    return timeout;
  }

  @Override
  public void setTimeout(int timeout)
  {
    this.timeout = timeout;
  }

  public MatrixSqlGenerator getMatrixSqlGenerator()
  {
    return matrixSqlGenerator;
  }

  public void setMatrixSqlGenerator(MatrixSqlGenerator matrixSqlGenerator)
  {
    this.matrixSqlGenerator = matrixSqlGenerator;
  }

  public SaltAnnotateExtractor getSaltAnnotateExtractor()
  {
    return saltAnnotateExtractor;
  }

  public void setSaltAnnotateExtractor(
    SaltAnnotateExtractor saltAnnotateExtractor)
  {
    this.saltAnnotateExtractor = saltAnnotateExtractor;
  }

  public ByteHelper getByteHelper()
  {
    return byteHelper;
  }

  public void setByteHelper(ByteHelper byteHelper)
  {
    this.byteHelper = byteHelper;
  }

  @Override
  public InputStream getBinary(String toplevelCorpusName, String corpusName,
    String mimeType, String title, int offset, int length)
  {
    // correct the API inconsistency
    offset = offset - 1;

    AnnisBinaryMetaData binary =
      (AnnisBinaryMetaData) getJdbcTemplate().query(ByteHelper.SQL,
      byteHelper.
      getArgs(toplevelCorpusName, corpusName, mimeType, title, offset,
      length),
      ByteHelper.getArgTypes(), byteHelper);

    try
    {
      // retrieve the requested part of the file from the data directory
      File dataFile = new File(getRealDataDir(), binary.getLocalFileName());

      long fileSize = dataFile.length();

      // do not make the array bigger as necessary
      length = (int) Math.min(fileSize - (long) offset, (long) length);

      FileInputStream fInput = new FileInputStream(dataFile);
      fInput.skip(offset);
      
      BoundedInputStream boundedStream = new BoundedInputStream(fInput, offset+length);

      return boundedStream;
    }
    catch (FileNotFoundException ex)
    {
      log.warn("Media file from database not found in data directory", ex);
    }
    catch (IOException ex)
    {
      log.warn("Error when readin media file from the data directory", ex);
    }

    return new ByteArrayInputStream(new byte[0]);
  }

  @Override
  public List<AnnisBinaryMetaData> getBinaryMeta(String toplevelCorpusName,
    String corpusName)
  {
    List<AnnisBinaryMetaData> metaData = getJdbcTemplate().query(
      MetaByteHelper.SQL,
      metaByteHelper.getArgs(toplevelCorpusName, corpusName),
      MetaByteHelper.getArgTypes(), metaByteHelper);

    // get the file size from the real file
    ListIterator<AnnisBinaryMetaData> it = metaData.listIterator();
    while (it.hasNext())
    {
      AnnisBinaryMetaData singleEntry = it.next();
      File f = new File(getRealDataDir(), singleEntry.getLocalFileName());
      singleEntry.setLength((int) f.length());
    }
    return metaData;
  }

  public AnnotateSqlGenerator<SaltProject> getAnnotateSqlGenerator()
  {
    return annotateSqlGenerator;
  }

  public void setAnnotateSqlGenerator(
    AnnotateSqlGenerator<SaltProject> annotateSqlGenerator)
  {
    this.annotateSqlGenerator = annotateSqlGenerator;
  }

  public CountSqlGenerator getCountSqlGenerator()
  {
    return countSqlGenerator;
  }

  public void setCountSqlGenerator(CountSqlGenerator countSqlGenerator)
  {
    this.countSqlGenerator = countSqlGenerator;
  }

  public MetaByteHelper getMetaByteHelper()
  {
    return metaByteHelper;
  }

  public void setMetaByteHelper(MetaByteHelper metaByteHelper)
  {
    this.metaByteHelper = metaByteHelper;
  }

  public String getExternalFilesPath()
  {
    return externalFilesPath;
  }

  public File getRealDataDir()
  {
    File dataDir;
    if (getExternalFilesPath() == null || getExternalFilesPath().isEmpty())
    {
      // use the default directory
      dataDir = new File(System.getProperty("user.home"), ".annis/data/");
    }
    else
    {
      dataDir = new File(getExternalFilesPath());
    }
    return dataDir;
  }

  public void setExternalFilesPath(String externalFilesPath)
  {
    this.externalFilesPath = externalFilesPath;
  }


  public ListExampleQueriesHelper getListExampleQueriesHelper()
  {
    return listExampleQueriesHelper;
  }

  public void setListExampleQueriesHelper(
    ListExampleQueriesHelper listExampleQueriesHelper)
  {
    this.listExampleQueriesHelper = listExampleQueriesHelper;
  }
}
