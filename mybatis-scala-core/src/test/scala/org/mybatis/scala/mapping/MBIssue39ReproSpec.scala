package org.mybatis.scala.mapping

import org.mybatis.scala.{Database, DatabaseSupport}
import org.mybatis.scala.domain.User
import org.mybatis.scala.infrastructure.UserRepository
import org.mybatis.scala.session.Session
import org.scalatest.{ShouldMatchers, FunSuite}

/**
  * Created by eyal on 26/11/2015.
  */
class MBIssue39ReproSpec extends FunSuite with ShouldMatchers with DatabaseSupport{
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

}

object MBIssue39ReproSpec{

  object Bad

}
