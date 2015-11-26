package org.mybatis.scala.mapping

import org.mybatis.scala.mapping.MBIssue39ReproSpec.{UserProps, UserId}
import org.mybatis.scala.{Database, DatabaseSupport}
import org.mybatis.scala.domain.User
import org.mybatis.scala.infrastructure.UserRepository
import org.mybatis.scala.session.Session
import org.scalatest.{ShouldMatchers, FunSuite}

/**
  * Created by eyal on 26/11/2015.
  */
class MBIssue39ReproSpec extends FunSuite with ShouldMatchers with DatabaseSupport{

  Database.config.addSpace("MBIssue39ReproSpec"){ space =>
    space ++= MBIssue39ReproSpec.Bad
    space ++= MBIssue39ReproSpec.Good
  }

  val users = for( i <- 1 to 10 ) yield{
    User( i, s"user$i", s"user$i@my.place" )
  }

  def setUpUsers( implicit session : Session ) ={
    for( user <- users ){
      UserRepository.create(user)
    }
  }

  private def withUsers(block: Session => Unit) =  withReadOnly(Database.default){ implicit session =>
    setUpUsers
    block( session )
  }

  test( "sanity" ){
    withUsers{ implicit session =>
      UserRepository.findAll().toSet shouldEqual  users.toSet
    }
  }

  test( "Bad" ){
    import MBIssue39ReproSpec.Bad
    withUsers{ implicit session =>
      val badUsers = Bad.selectUsers()
      badUsers should have size ( users.size )
    }
  }

  test( "Good" ){
    import MBIssue39ReproSpec.Good
    withUsers{ implicit session =>
      val goodUsers = Good.selectUsers()
      goodUsers should have size ( users.size )

      val us = for( gu <- goodUsers ) yield{
        val uid = gu.userId
        val userProps = Option( gu.userProps ) getOrElse  new MBIssue39ReproSpec.UserProps
        val user = new User( uid, userProps.userName, userProps.email )
        user
      }
      us.toSet shouldEqual users.toSet

    }
  }

}

object MBIssue39ReproSpec{

  class UserId{
    var userId : Int = _
  }
  class UserProps{
    var userName : String = _
    var email : String = _
  }

  object UserIdMapping extends ResultMap[UserId]{
    result( "userId", "id", T[Int])
  }
  object UserPropsMapping extends ResultMap[UserProps]{
    result( "userName", "name", T[String])
    result( "email", "email", T[String])
  }

  object Bad{

    class User{
      var userId : UserId = _
      var userProps : UserProps = _
    }

    object UserMapping extends ResultMap[User]{
      association("userId",resultMap = UserIdMapping )
      association("userProps", resultMap = UserPropsMapping)
    }

    val selectUsers = new SelectList[User] {
      resultMap = UserMapping
      override def xsql: XSQL = <xsql>SELECT * FROM user</xsql>
    }

    def bind = Seq( selectUsers )
  }

  object Good{
    class User extends UserId{
      var userProps : UserProps = _
    }
    object UserMapping extends ResultMap[User]( UserIdMapping ){
      association("userProps", resultMap = UserPropsMapping)
    }

    val selectUsers = new SelectList[User] {
      resultMap = UserMapping
      override def xsql: XSQL = <xsql>SELECT * FROM user</xsql>
    }

    def bind = Seq( selectUsers )
  }
}
