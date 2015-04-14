import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.*;


public class TTPServer implements Runnable {
   Socket sock= null;
   String userId;
   String providerId;
   String USERTOKEN_QUERY = "SELECT userKey FROM userKeys WHERE userId = "+userId+" AND providerId = "+providerId;
   String user;
   String pass;

   public TTPServer(Socket _sock, String _user, String _pass) {
      sock = _sock;
	  user=_user;
	  pass=_pass;
   }

   public void run() {
      try {
         BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
         String request = in.readLine();
         System.out.println(request);
         String query = makeQuery(request);
         System.out.println(query);

         Class.forName("com.mysql.jdbc.Driver").newInstance();
          Connection con = DriverManager.getConnection("jdbc:mysql://cs.okstate.edu:3306/"+user, user, pass);

         ResultSet rs = con.createStatement().executeQuery(query);
         rs.next();
         String _userKey = rs.getString("userKey");
         System.out.println(userId + "\t" + providerId + "\t" + _userKey);

         con.close();

         sock.getOutputStream().write(_userKey.getBytes());
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         try {
            sock.close();
         } catch (IOException e1) {
            e1.printStackTrace();
            sock=null;
         }
      }
   }

   private String makeQuery(String request) {
      String[] fields = request.split(",");
      userId = fields[0];
      providerId = fields[1];
      USERTOKEN_QUERY = "SELECT userKey FROM userKeys WHERE userId = "+userId+" AND providerId = "+providerId;
      return USERTOKEN_QUERY;
   }

   public static void main(String[] args) {
      ServerSocket ss = null;

      try {
         ss = new ServerSocket(8080);
      }
      catch (IOException e) {
         e.printStackTrace();
         System.out.println("Unable to create TTP Server");
         return;
      }

      while(true) {
         try {
            Thread t = new Thread(new TTPServer(ss.accept(), args[0], args[1]));
            t.start();
         } catch (IOException e) {
            System.out.println("Unable to accept new connection.");
            try {
               ss.close();
            } catch (IOException e1) {
               e1.printStackTrace();
               ss=null;
            }
         } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
		    System.out.println("java TTPServer <db username> <password>");
		 }
      }
   }
}
