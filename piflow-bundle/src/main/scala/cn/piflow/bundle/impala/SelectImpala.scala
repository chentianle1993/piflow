package cn.piflow.bundle.impala

import java.sql.{Connection, DriverManager, ResultSet, Statement}

import cn.piflow.{JobContext, JobInputStream, JobOutputStream, ProcessContext}
import cn.piflow.conf.{ConfigurableStop, PortEnum, StopGroup}
import cn.piflow.conf.bean.PropertyDescriptor
import cn.piflow.conf.util.{ImageUtil, MapUtil}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

import scala.collection.mutable.ArrayBuffer


class SelectImpala extends ConfigurableStop{
  override val authorEmail: String = "yangqidong@cnic.cn"
  override val description: String = "get data from impala"
  val inportList: List[String] = List(PortEnum.NonePort.toString)
  val outportList: List[String] = List(PortEnum.DefaultPort.toString)

  var url:String=_
  var user:String=_
  var password:String=_
  var sql:String=_
  var schameString : String=_


    override def perform(in: JobInputStream, out: JobOutputStream, pec: JobContext): Unit =  {
    val session: SparkSession = pec.get[SparkSession]()

      Class.forName("org.apache.hive.jdbc.HiveDriver")

      val con: Connection = DriverManager.getConnection("jdbc:hive2://"+url+"/;auth=noSasl",user,password)
      val stmt: Statement = con.createStatement()
      val rs: ResultSet = stmt.executeQuery(sql)

      val filedNames: Array[String] = schameString.split(",")
      var rowsArr:ArrayBuffer[ArrayBuffer[String]]=ArrayBuffer()
      while (rs.next()){
        var rowArr:ArrayBuffer[String]=ArrayBuffer()
        for(fileName <- filedNames){
          rowArr+=rs.getString(fileName)
        }
        rowsArr+=rowArr
      }

      val fields: Array[StructField] = filedNames.map(d=>StructField(d,StringType,nullable = true))
      val schema: StructType = StructType(fields)

      val rows: List[Row] = rowsArr.toList.map(arr => {
        val row: Row = Row.fromSeq(arr)
        row
      })
      val rdd: RDD[Row] = session.sparkContext.makeRDD(rows)
      val df: DataFrame = session.createDataFrame(rdd,schema)

      out.write(df)

  }

  override def setProperties(map: Map[String, Any]): Unit = {
    url = MapUtil.get(map,"url").asInstanceOf[String]
    user = MapUtil.get(map,"user").asInstanceOf[String]
    password = MapUtil.get(map,"password").asInstanceOf[String]
    sql = MapUtil.get(map,"sql").asInstanceOf[String]
    schameString = MapUtil.get(map,"schameString").asInstanceOf[String]

  }


  override def getPropertyDescriptor(): List[PropertyDescriptor] = {
    var descriptor : List[PropertyDescriptor] = List()

    val url=new PropertyDescriptor().name("url").displayName("url").description("IP and port number, you need to write like this -- ip:port").defaultValue("").required(true)
    descriptor = url :: descriptor
    val user=new PropertyDescriptor().name("user").displayName("user").description("user").defaultValue("").required(false)
    descriptor = user :: descriptor
    val password=new PropertyDescriptor().name("password").displayName("password").description("password").defaultValue("").required(false)
    descriptor = password :: descriptor
    val sql=new PropertyDescriptor().name("sql").displayName("sql").description("The name of the table has not changed.But you have to specify which database," +
      " such as database.table.").defaultValue("").required(true)
    descriptor = sql :: descriptor
    val schameString=new PropertyDescriptor().name("schameString").displayName("schameString").description("The field of SQL statement query results is divided by ,").defaultValue("").required(true)
    descriptor = schameString :: descriptor

    descriptor
  }

  override def getIcon(): Array[Byte] = {
    ImageUtil.getImage("impala/impala-logo.png")
  }

  override def getGroup(): List[String] = {
    List(StopGroup.Mongodb.toString)
  }


  override def initialize(ctx: ProcessContext): Unit = {
  }

}
