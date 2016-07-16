/**
 * RUBBoS: Rice University Bulletin Board System.
 * Copyright (C) 2001-2004 Rice University and French National Institute For
 * Research In Computer Science And Control (INRIA).
 * Contact: jmob@objectweb.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * Initial developer(s): Emmanuel Cecchet.
 * Contributor(s): ______________________.
 */

package edu.rice.rubbos.servlets;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.GregorianCalendar;

import javax.servlet.http.HttpServletResponse;

/**
 * In fact, this class is not a servlet itself but it provides output services
 * to servlets to send back HTML files.
 *
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet </a> and <a
 *         href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite </a>
 * @version 1.0
 */

public class ServletPrinter
{
  private PrintWriter       out;
  private String            servletName;
  private GregorianCalendar startDate;

  public ServletPrinter(HttpServletResponse toWebServer,
      String callingServletName)
  {
    startDate = new GregorianCalendar();
    toWebServer.setContentType("text/html");
    try
    {
      out = toWebServer.getWriter();
    }
    catch (IOException ioe)
    {
      ioe.printStackTrace();
    }
    servletName = callingServletName;
  }

  void printFile(String filename)
  {
    FileReader fis = null;
    try
    {
      fis = new FileReader(filename);
      char[] data = new char[4 * 1024]; // 4K buffer
      int bytesRead;
      bytesRead = fis.read(data);
      while (/* (bytesRead = fis.read(data)) */bytesRead != -1)
      {
        out.write(data, 0, bytesRead);
        bytesRead = fis.read(data);
      }
    }
    catch (Exception e)
    {
      out.println("Unable to read file (exception: " + e + ")<br>");
    }
    finally
    {
      if (fis != null)
        try
        {
          fis.close();
        }
        catch (Exception ex)
        {
          out.println("Unable to close the file reader (exception: " + ex
              + ")<br>");
        }
    }
  }

  void printHTMLheader(String title)
  {
    printFile(Config.HTMLFilesPath + "/header.html");
    out.println("<title>" + title + "</title>");
  }

  void printHTMLfooter()
  {
    GregorianCalendar endDate = new GregorianCalendar();

    out
        .println("<br><hr>RUBBoS (C) Rice University/INRIA<br><i>Page generated by "
            + servletName
            + " in "
            + TimeManagement.diffTime(startDate, endDate) + "</i><br>");
    out.println("</body>");
    out.println("</html>");
  }

  void printHTML(String msg)
  {
    out.println(msg);
  }

  void printHTMLHighlighted(String msg)
  {
    out.println("<TABLE width=\"100%\" bgcolor=\"#CCCCFF\">");
    out
        .println("<TR><TD align=\"center\" width=\"100%\"><FONT size=\"4\" color=\"#000000\"><B>"
            + msg + "</B></FONT></TD></TR>");
    out.println("</TABLE><p>");
  }

  public String authenticate(String nickname, String password, Connection conn)
  {
    try
    {
      PreparedStatement stmt = conn
          // Yasu: for PostgreSQL
          // .prepareStatement("SELECT id FROM users WHERE nickname=\"" + nickname
          //     + "\" AND password=\"" + password + "\"",
          .prepareStatement("SELECT id FROM users WHERE nickname='" + nickname
              + "' AND password='" + password + "'",
            // Yasu: FORWARD_ONLY ResultSet doesn't support first() method.
	    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
      ResultSet rs = stmt.executeQuery();
      rs.first();
      if (!rs.first())
        return "0"; // 0 is the anonymous user
      return rs.getString("id");

    }
    catch (Exception e)
    {
      return e + "Authenticate function error";

    }
  }

  public String getUserName(int UserId, Connection conn) throws Exception
  {
    try
    {
      PreparedStatement stmt = conn
          .prepareStatement("SELECT nickname FROM users WHERE id=?",
            // Yasu: FORWARD_ONLY ResultSet doesn't support first() method.
	    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
      stmt.setInt(1, UserId);
      ResultSet rs = stmt.executeQuery();
      rs.first();
      return rs.getString("nickname");
    }
    catch (Exception e)
    {
      throw e;
    }

  }

}
