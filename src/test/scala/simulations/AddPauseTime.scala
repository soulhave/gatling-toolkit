package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt

class AddPauseTime extends Simulation{

  // 1. Http Conf
  val httpConf = http.baseUrl("http://localhost:8080/app/")
    .header("Accept", "application/json")

  // 2. Scenario definition
  val scn = scenario("Video Game DB - 3 calls")

    .exec(http("Get all video games - 1st call")
      .get("videogames"))
    .pause(5)

    .exec(http("Get specific game")
      .get("videogames/1"))
    .pause(1, 2)

    .exec(http("Get All video games - 2nd call")
      .get("videogames"))
    .pause(3000.milliseconds)

  // 3. Load scenario
  setUp(
    scn.inject(atOnceUsers((1)))
  ).protocols(httpConf)

}
