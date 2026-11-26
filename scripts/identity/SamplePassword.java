import org.springframework.security.crypto.bcrypt.BCrypt;
class SamplePassword {
 public static void main(String[] args) {
   if(args.length==2 && args[0].equals("verify")) System.out.print(BCrypt.checkpw("123456",args[1]));
   else System.out.print(BCrypt.hashpw("123456", BCrypt.gensalt(10)));
 }
}
