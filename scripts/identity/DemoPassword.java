import org.springframework.security.crypto.bcrypt.BCrypt;
class DemoPassword {
 public static void main(String[] args) { System.out.print(BCrypt.hashpw("Demo@123456", BCrypt.gensalt(10))); }
}
