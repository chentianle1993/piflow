package cn.piflow.bundle.microorganism

import java.io._

import cn.piflow.bundle.microorganism.util.BioProject
import cn.piflow.conf.bean.PropertyDescriptor
import cn.piflow.conf.util.{ImageUtil, MapUtil}
import cn.piflow.conf.{ConfigurableStop, PortEnum, StopGroup}
import cn.piflow.{JobContext, JobInputStream, JobOutputStream, ProcessContext}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FSDataInputStream, FSDataOutputStream, FileSystem, Path}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.json.{JSONArray, JSONObject, XML}

class BioSampleParse extends ConfigurableStop{
  val authorEmail: String = "ygang@cnic.cn"
  val description: String = "Parsing BioSample type data"
  val inportList: List[String] = List(PortEnum.DefaultPort.toString)
  val outportList: List[String] = List(PortEnum.DefaultPort.toString)

  var cachePath:String = _

  var docName = "BioSample"
  def perform(in: JobInputStream, out: JobOutputStream, pec: JobContext): Unit = {
    val spark = pec.get[SparkSession]()
    val sc = spark.sparkContext
    val ssc = spark.sqlContext
    val inDf= in.read()

    val configuration: Configuration = new Configuration()
    var pathStr: String =inDf.take(1)(0).get(0).asInstanceOf[String]
    val pathARR: Array[String] = pathStr.split("\\/")
    var hdfsUrl:String=""
    for (x <- (0 until 3)){
      hdfsUrl+=(pathARR(x) +"/")
    }

    configuration.set("fs.defaultFS",hdfsUrl)
    var fs: FileSystem = FileSystem.get(configuration)

    var hdfsPathJsonCache:String = ""

    var fdosOut: FSDataOutputStream = null
    var bisIn: BufferedInputStream =null

    var count = 0
    var nameNum = 0
    inDf.collect().foreach(row => {
      pathStr = row.get(0).asInstanceOf[String]

      var line: String = null
      var xml = ""
      var fdis: FSDataInputStream = fs.open(new Path(pathStr))
      val br: BufferedReader = new BufferedReader(new InputStreamReader(fdis))
      br.readLine()
      br.readLine()


      while ((line = br.readLine()) != null && line!= null) {
        xml = xml + line
        if (line.indexOf("</" + docName + ">") != -1) {
          count = count + 1
          val doc: JSONObject = XML.toJSONObject(xml).getJSONObject(docName)

          val accession = doc.optString("accession")
          // Attributes
          val attrs: String = doc.optString("Attributes")
          if (attrs.equals("")) {
            doc.remove("Attributes")
          }
          // Links
          val links: String = doc.optString("Links")
          if (links != null) {
            if (links.equals("")) {
              doc.remove("Links")
            }
          }
          val bio = new BioProject
          // owner.name
          val owner = doc.optString("Owner")
          if (owner != null) {
            if (owner.isInstanceOf[JSONArray]) for (k <- 0 until owner.toArray.length) {

              val singleOwner: JSONObject = owner(k).asInstanceOf[JSONObject]
              bio.convertConcrete2KeyVal(singleOwner, "Name")
            }
          }
          // Models.Model
          val models = doc.optJSONObject("Models")
          if (models != null) {
            bio.convertConcrete2KeyVal(models, "Models")
          }
          if (count%200000 == 1 ){
            nameNum += 1
            hdfsPathJsonCache = hdfsUrl+cachePath+"/biosampleCache/"+"biosample"+nameNum+".json"

            val path: Path = new Path(hdfsPathJsonCache)
            if(fs.exists(path)){
              fs.delete(path)
            }
            fs.create(path).close()
            fdosOut = fs.append(path)

            bisIn = new BufferedInputStream(new ByteArrayInputStream(("[" + doc.toString).getBytes()))
            val buff: Array[Byte] = new Array[Byte](1048576)
            var num: Int = bisIn.read(buff)
            while (num != -1) {
              fdosOut.write(buff, 0, num)
              fdosOut.flush()
              num = bisIn.read(buff)
            }
            bisIn.close()
          } else if (count%200000 ==0){
            bisIn = new BufferedInputStream(new ByteArrayInputStream((","+doc.toString + "]").getBytes()))
            val buff: Array[Byte] = new Array[Byte](1048576)
            var num: Int = bisIn.read(buff)
            while (num != -1) {
              fdosOut.write(buff, 0, num)
              fdosOut.flush()
              num = bisIn.read(buff)
            }
            fdosOut.flush()
            fdosOut.close()
            bisIn.close()

          } else {
            bisIn = new BufferedInputStream(new ByteArrayInputStream(("," + doc.toString).getBytes()))
            val buff: Array[Byte] = new Array[Byte](1048576)
            var num: Int = bisIn.read(buff)
            while (num != -1) {
              fdosOut.write(buff, 0, num)
              fdosOut.flush()
              num = bisIn.read(buff)
            }
            bisIn.close()
          }
          xml = ""
        }
      }
    })
    if (count%200000 != 0){
      bisIn = new BufferedInputStream(new ByteArrayInputStream(("]").getBytes()))
      val buff: Array[Byte] = new Array[Byte](1048576)
      var num: Int = bisIn.read(buff)
      while (num != -1) {
        fdosOut.write(buff, 0, num)
        fdosOut.flush()
        num = bisIn.read(buff)
      }
      fdosOut.flush()
      bisIn.close()
    }
    fdosOut.close()
    println("start parser HDFSjsonFile")
    val df: DataFrame = ssc.read.json(hdfsUrl+cachePath+"/biosampleCache/")
    out.write(df)
  }

  def setProperties(map: Map[String, Any]): Unit = {
    cachePath=MapUtil.get(map,key="cachePath").asInstanceOf[String]
  }

  override def getPropertyDescriptor(): List[PropertyDescriptor] = {
    var descriptor : List[PropertyDescriptor] = List()
    val cachePath = new PropertyDescriptor().name("cachePath").displayName("cachePath").defaultValue("").required(true)
    descriptor = cachePath :: descriptor
    descriptor
  }

  override def getIcon(): Array[Byte] = {
    ImageUtil.getImage("microorganism/png/Biosample.png")
  }

  override def getGroup(): List[String] = {
    List(StopGroup.MicroorganismGroup.toString)
  }

  def initialize(ctx: ProcessContext): Unit = {

  }


}
