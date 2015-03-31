package uk.co.real_logic.aeron.tools;

/**
 * Stats associated to a publishing channel.
 */
public class PublisherStats extends TransportStats
{
  private long limit;

  /**
   * Constructor that takes in the publisher info from the label buffer
   *
   * @param channel
   */
  public PublisherStats(String channel)
  {
    parseChannel(channel);
    active = true;
  }

  /**
   * Update the limit
   *
   * @param limit
   */
  public void setLimit(long limit)
  {
    if (limit != this.limit)
    {
      this.limit = limit;
      active = true;
    }
  }

  /**
   * Convert object to string representation for output.
   *
   * @return String
   */
  public String toString()
  {
    String s = String.format("%1$5s %2$8d %3$8d %4$10s:%5$5d %6$s%7$s %8$8s\n",
        proto, pos, limit, host, port, "0x", sessionId, active ? "ACTIVE" : "INACTIVE");
    active = false;

    return s;
  }
}
