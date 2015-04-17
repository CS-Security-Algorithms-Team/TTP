import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class TTPClient {

   public static void main(String[] args) {
      try {
         Socket sock = new Socket("localhost", 8080);

         PrintWriter out = new PrintWriter(sock.getOutputStream());
         out.println(args[0]+","+args[1]+","+args[2]);
         out.flush();
         BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

         String line=null;
         while((line =in.readLine()) != null) {
            System.out.println(line);
         }
         sock.close();
      } catch (IOException e) {
         e.printStackTrace();
      } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
         System.out.println("java TTPClient <username> <password> <providerId>");
      }


   }

}
