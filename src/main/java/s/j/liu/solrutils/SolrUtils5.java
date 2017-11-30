package s.j.liu.solrutils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;

/**
 * @version v0.0.1
 * @since 2017-06-29 09:29:00
 * @author Shengjun Liu
 *
 */
@SuppressWarnings("deprecation")
public class SolrUtils5<T> {
  public static String Q = "q";
  public static String FQ = "fq";
  public static String SORT = "sort";
  public static String START = "start";
  public static String ROWS = "rows";
  public static String FL = "fl";
  public static String DF = "df";
  /** json | xml | python | ruby | php | csv . */
  public static String WT = "wt";
  /** off | on . */
  public static String INDEX = "index";

  private String solrUrl = null;
  private HttpSolrClient httpSolrClient = null;
  private Class<T> clazz = null;
  private Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
  private long localCacheSize = 100L;
  private int step = 10000;

  /**
   * Construction Method.
   * 
   * @param solrUrl
   *          solr address:http://127.0.0.1:8983/solr
   * @param core
   *          solr's collection name,eg: collection
   * @param step
   *          每次查询步长
   * @param clazz
   *          Class
   */
  public SolrUtils5(String solrUrl, String core, int step, Class<T> clazz) {
    this.step = step;
    this.clazz = clazz;
    setSorlAddress(solrUrl, core);
  }

  /**
   * Construction Method.
   * 
   * @param solrUrl
   *          solr address:http://127.0.0.1:8983/solr
   * @param core
   *          solr's collection name,eg: collection
   * @param clazz
   *          Class
   */
  public SolrUtils5(String solrUrl, String core, Class<T> clazz) {
    this.clazz = clazz;
    setSorlAddress(solrUrl, core);
  }

  /**
   * 初始化solr连接，
   * 
   * @param solrUrl
   *          solr address:http://127.0.0.1:8983/solr
   * @param core
   *          solr's collection name,eg: collection
   */
  public void setSorlAddress(String solrUrl, String core) {
    this.solrUrl = solrUrl.endsWith("/") ? solrUrl : solrUrl + "/";
    this.solrUrl = (core == null) ? this.solrUrl : this.solrUrl + core + "/";
    getHttpSolrClient();
  }

  /**
   * set SolrInputDocument Local Cache Size.
   * 
   * @param size
   *          Size
   * @return SolrUtils5
   */
  public SolrUtils5<?> setLocalCacheSize(long size) {
    this.localCacheSize = size;
    return this;
  }

  /**
   * Get List.
   * 
   * @param condition
   *          Condition
   * @return List
   */
  public List<T> getTList(Map<String, String> condition) {
    long numFound = getSolrDocumentList(condition, 0, 1).getNumFound();
    List<T> list = new ArrayList<T>();
    for (int j = 0; j < numFound; j += step) {
      SolrDocumentList solrDocumentList = getSolrDocumentList(condition, j, step);
      for (SolrDocument solrDocument : solrDocumentList) {
        @SuppressWarnings("unchecked")
        T object = (T) toBean(solrDocument, clazz);
        if (object != null) {
          list.add(object);
        }
      }
    }
    return list;
  }

  /**
   * Get List.
   * 
   * @param condition
   *          Condition
   * @param startIndex
   *          Start Index
   * @param rowsNumber
   *          Rows Number
   * @return List
   */
  public List<T> getTList(Map<String, String> condition, int startIndex, int rowsNumber) {
    List<T> list = new ArrayList<T>();
    SolrDocumentList solrDocumentList = getSolrDocumentList(condition, startIndex, rowsNumber);
    for (SolrDocument solrDocument : solrDocumentList) {
      @SuppressWarnings("unchecked")
      T object = (T) toBean(solrDocument, clazz);
      if (object != null) {
        list.add(object);
      }
    }
    return list;
  }

  /**
   * Get List.
   * 
   * @param condition
   *          Condition
   * @param fields
   *          The result field
   * @return List
   */
  public List<Map<String, Object>> getList(Map<String, String> condition, List<String> fields) {
    long numFound = getSolrDocumentList(condition, 0, 1).getNumFound();
    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    for (int j = 0; j < numFound; j += step) {
      SolrDocumentList solrDocumentList = getSolrDocumentList(condition, j, step);
      for (SolrDocument solrDocument : solrDocumentList) {
        Map<String, Object> map = new HashMap<String, Object>();
        fields.forEach(field -> {
          map.put(field, solrDocument.get(field));
        });
        list.add(map);
      }
    }
    return list;
  }

  /**
   * Get List.
   * 
   * @param condition
   *          Condition
   * @param startIndex
   *          Start Index
   * @param rowsNumber
   *          Rows Number
   * @param fields
   *          The result field
   * @return List
   */
  public List<Map<String, Object>> getList(Map<String, String> condition, int startIndex,
      int rowsNumber, List<String> fields) {
    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    SolrDocumentList solrDocumentList = getSolrDocumentList(condition, startIndex, rowsNumber);
    for (SolrDocument solrDocument : solrDocumentList) {
      Map<String, Object> map = new HashMap<String, Object>();
      fields.forEach(field -> {
        map.put(field, solrDocument.get(field));
      });
      list.add(map);
    }
    return list;
  }

  /**
   * Get List.
   * 
   * @param condition
   *          Condition
   * @param field
   *          The result field
   * @return List
   */
  public List<Object> getList(Map<String, String> condition, String field) {
    long numFound = getSolrDocumentList(condition, 0, 1).getNumFound();
    List<Object> list = new ArrayList<Object>();
    for (int j = 0; j < numFound; j += step) {
      SolrDocumentList solrDocumentList = getSolrDocumentList(condition, j, step);
      for (SolrDocument solrDocument : solrDocumentList) {
        list.add(solrDocument.get(field));
      }
    }
    return list;
  }

  /**
   * Get List.
   * 
   * @param condition
   *          Condition
   * @param startIndex
   *          Start Index
   * @param rowsNumber
   *          Rows Number
   * @param field
   *          The result field
   * @return List
   */
  public List<Object> getList(Map<String, String> condition, int startIndex, int rowsNumber,
      String field) {
    List<Object> list = new ArrayList<Object>();
    SolrDocumentList solrDocumentList = getSolrDocumentList(condition, startIndex, rowsNumber);
    for (SolrDocument solrDocument : solrDocumentList) {
      list.add(solrDocument.get(field));
    }
    return list;
  }

  /**
   * Get Result Size.
   * 
   * @param condition
   *          Condition
   * @return long
   */
  public long getResultSize(Map<String, String> condition) {
    SolrDocumentList solrDocumentList = getSolrDocumentList(condition, 0, 1);
    return solrDocumentList.getNumFound();
  }

  /**
   * Add doc 2 Local Cache.
   * 
   * @param doc
   *          SolrInputDocument
   */
  public void addIndexToLocalCache(SolrInputDocument doc) {
    synchronized (docs) {
      docs.add(doc);
      if (docs.size() > localCacheSize) {
        addIndexToSolr();
      }
    }
  }

  /**
   * Send docs 2 solr.
   */
  public void addIndexToSolr() {
    synchronized (docs) {
      try {
        httpSolrClient.add(docs);
        clearIndexToLocalCache();
      } catch (SolrServerException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Clear Local Cache docs.
   */
  public void clearIndexToLocalCache() {
    synchronized (docs) {
      docs.clear();
    }
  }

  /**
   * Get SolrDocumentList according to the condition.
   * 
   * @param condition
   *          Condition
   * @param startIndex
   *          Start Index
   * @param rowsNumber
   *          Rows Number
   * @return SolrDocumentList
   */
  private SolrDocumentList getSolrDocumentList(final Map<String, String> condition,
      final int startIndex, final int rowsNumber) {
    SolrDocumentList documentList = getQueryResponse(condition, startIndex, rowsNumber)
        .getResults();
    return documentList;
  }

  /**
   * Get QueryResponse according to the condition.
   * 
   * @param condition
   *          Condition
   * @param startIndex
   *          Start Index
   * @param rowsNumber
   *          Rows Number
   * @return QueryResponse
   */
  private QueryResponse getQueryResponse(final Map<String, String> condition, final int startIndex,
      final int rowsNumber) {
    SolrQuery solrQuery = new SolrQuery();
    Iterator<Entry<String, String>> iterator = condition.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<String, String> entry = iterator.next();
      solrQuery.set(entry.getKey(), entry.getValue());
    }
    solrQuery.setStart(startIndex);
    solrQuery.setRows(rowsNumber);
    QueryResponse queryResponse = null;
    try {
      queryResponse = this.httpSolrClient.query(solrQuery, METHOD.POST);
      return queryResponse;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Get HttpSolrClient.
   * 
   * @return HttpSolrClient
   */
  private HttpSolrClient getHttpSolrClient() {
    try {
      this.httpSolrClient = new HttpSolrClient.Builder(this.solrUrl).build();
      this.httpSolrClient.setParser(new XMLResponseParser());
      this.httpSolrClient.setConnectionTimeout(10000);
      this.httpSolrClient.setAllowCompression(true);
      this.httpSolrClient.setMaxTotalConnections(10000);
      this.httpSolrClient.setDefaultMaxConnectionsPerHost(10000);
      this.httpSolrClient.setFollowRedirects(false);
    } catch (SolrException e) {
      this.httpSolrClient = new HttpSolrClient(this.solrUrl, new SystemDefaultHttpClient());
      this.httpSolrClient.setParser(new XMLResponseParser());
      this.httpSolrClient.setConnectionTimeout(10000);
      this.httpSolrClient.setAllowCompression(true);
      this.httpSolrClient.setMaxTotalConnections(10000);
      this.httpSolrClient.setDefaultMaxConnectionsPerHost(10000);
      this.httpSolrClient.setFollowRedirects(false);
    }
    return this.httpSolrClient;
  }

  /**
   * Get HttpSolrClient.
   * 
   * @return
   */
  public HttpSolrClient getClient() {
    return this.httpSolrClient;
  }

  /**
   * Close Resource.
   */
  public void closeResource() {
    try {
      this.httpSolrClient.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * SolrDocument to Bean.
   * 
   * @param record
   *          Solr Document
   * @param clazz
   *          Target Object
   * @return Object
   */
  public Object toBean(final SolrDocument record, final Class<?> clazz) {
    Object obj = null;
    try {
      obj = clazz.newInstance();
    } catch (InstantiationException e1) {
      e1.printStackTrace();
    } catch (IllegalAccessException e1) {
      e1.printStackTrace();
    }
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      try {
        Object value = record.get(field.getName());
        BeanUtils.setProperty(obj, field.getName(), value);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }
    return obj;
  }
}