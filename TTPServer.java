import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.*;


public class TTPServer implements Runnable {
   Socket sock= null;
   String username;
   String password;
   String providerId;
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
		 if(request == null) {
			sock.getOutputStream().write("Authentication failed, must provide username, password, providerId".getBytes());
			return;
		 }
         System.out.println(request);
         String query = makePasswordQuery(request);
		 if(query.equals("")) {
			sock.getOutputStream().write("Authentication failed, must provide username, password, providerId".getBytes());
		 }
		 System.out.println(query);
		 
		 Class.forName("com.mysql.jdbc.Driver").newInstance();
          Connection con = DriverManager.getConnection("jdbc:mysql://cs.okstate.edu:3306/"+user, user, pass);
		  
		 ResultSet rs = con.createStatement().executeQuery(query);
		 if(rs.next()==false) {
			sock.getOutputStream().write("Authentication failed, cannot get user".getBytes());
			return;
		 }
         String dbPassword = rs.getString("password");
         String userId = rs.getString("id");
		 
		 if(password.equals(dbPassword)) {
			query = makeTokenQuery(userId);
			System.out.println(query);
		 } else {
		    sock.getOutputStream().write("Authentication failed, bad password".getBytes());
			return;
		 }

         rs = con.createStatement().executeQuery(query);
         if(rs.next()==false) {
			sock.getOutputStream().write("Authentication failed, bad provider id".getBytes());
			return;
		 }
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

   private String makePasswordQuery(String request) {
      String[] fields = request.split(",");
	  if(fields ==null || fields.length!=3) {
		return "";
	  }
      username = fields[0];
	  password = fields[1];
      providerId = fields[2];
      String query = "SELECT password, id FROM userData WHERE username = '"+username+"'";
      return query;
   }
   
    private String makeTokenQuery(String userId) {
      String query = "SELECT userKey FROM userKeys WHERE userId = "+userId+" AND providerId = "+providerId;
      return query;
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
