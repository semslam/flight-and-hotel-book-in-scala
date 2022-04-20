package models.admin

class QueuesType {

}

object QueuesType {

  def getQueues() = {
    List("AWAITING_ISSUE", "RE_ISSUE", "DATE_CHANGED")
  }
}