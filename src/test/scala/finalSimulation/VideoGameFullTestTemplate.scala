package finalSimulation

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import scala.util.Random

class VideoGameFullTestTemplate extends Simulation{

  val httpConf = http.baseUrl("http://192.168.0.20:8080/app/")
    .header("Accept", "application/json")
//    .proxy(Proxy("localhost", 8866))

  /** Variables */
  //VARS
  var idNumbers = (11 to 21).iterator
  var idToDelete =(11 to 21).iterator
  val rnd = new Random()
  val now = LocalDate.now()
  val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  /** Helper methods */
  def randomString(lenght: Int) = {
    rnd.alphanumeric.filter(_.isLetter).take(lenght).mkString
  }

  def getRandomDate(startDate: LocalDate, random: Random): String = {
    startDate.minusDays(random.nextInt(30)).format(pattern)
  }

  /** Custom Feeder */
  //FEEDER
  val customFeeder = Iterator.continually(Map(
    "idGame" -> idNumbers.next(),
    "name" -> ("Game - " + randomString(5)),
    "releaseDate" -> getRandomDate(now, rnd),
    "reviewScore" -> rnd.nextInt(100),
    "category" -> ("Category - " + randomString(6)),
    "rating" -> ("Rating - " + randomString(4))
  ))

  val csvFeeder = csv("data/gameCsvFile.csv").random
  val idToDeleteFeeder = Iterator.continually(Map("idGame" -> idToDelete.next()))

  /*** HTTP CALLS ***/
  // GET ALL VIDEO GAMES
  def getAllVideoGames() = {
    exec(
      http("Get all video games")
        .get("videogames/")
        .check(status.is(200))
    )
  }

  // CREATE NEW VIDEO GAMES
  def createNewVideoGame() = {
    feed(customFeeder)
      .exec(
        http("Create a new video game")
          .post("videogames/")
          .body(ElFileBody("templates/VideoGame.json")).asJson
      )
  }

  // GET SPECIFIC VIDEO GAMES
  def getDetailsOfThatSingle() = {
    feed(csvFeeder)
      .exec(
        http("Get specific game")
          .get("videogames/${gameId}")
          .check(status.is(200))
          .check(jsonPath("$.name").is("${gameName}"))
      )
  }

  def deleteVideoGame() = {
    feed(idToDeleteFeeder)
      .exec(
        http("Get specific game")
          .delete("videogames/${idGame}")
          .check(status.is(200))
      )
  }

  // add other calls here

  /** SCENARIO DESIGN */
  // using the http call, create a scenario that does the following:
  // 1. Get all games
  // 2. Create new Game
  // 3. Get details of that single
  // 4. Delete the game
  val scn = scenario("Full Test Template")
    .exec(getAllVideoGames())
    .pause(5)

    .exec(createNewVideoGame())
    .pause(5)

    .exec(getDetailsOfThatSingle())
    .pause(5)

    .exec(deleteVideoGame())


  /** SETUP LOAD SIMULATION */
  private def getProperty(propertyName: String, defaultValue: String) = {
    Option(System.getenv(propertyName))
      .orElse(Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }

  def userCount: Int = getProperty("USERS", "5").toInt
  def rampDuration: Int = getProperty("RAMP_DURATION", "10").toInt
  def testDuration: Int = getProperty("DURATION", "60").toInt

  setUp(
    scn.inject(
      nothingFor(5),
      rampUsers(userCount) during(rampDuration second)
    )
  ).protocols(httpConf)
    .maxDuration(testDuration)
      .assertions(
        global.responseTime.max.lt(200),
        global.successfulRequests.percent.gt(95)
      )


  /** Before & After */
  before {
    println(s"Running our test with ${userCount} users")
    println(s"Ramping users over ${rampDuration} seconds")
    println(s"Total test duration: ${testDuration} seconds")
  }

  after {
    println(s"Finished task!")
  }
}
