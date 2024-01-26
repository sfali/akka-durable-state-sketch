package projection

import akka.japi.function.Function
import akka.projection.jdbc.JdbcSession
import slick.jdbc.JdbcBackend.{Database, Session}

import java.sql.Connection

// TODO Find other spot for this
class SlickDbSession(db: Database) extends JdbcSession {
  private var currentSession: Option[Session] = None

  override def withConnection[Result](func: Function[Connection, Result]): Result = {
    val session = db.createSession()
    currentSession = Some(session)
    try {
      func.apply(session.conn)
    } finally {
      session.close()
      currentSession = None
    }
  }

  override def commit(): Unit = currentSession.foreach(_.conn.commit())

  override def rollback(): Unit = currentSession.foreach(_.conn.rollback())

  override def close(): Unit = currentSession.foreach(_.conn.close())
}
