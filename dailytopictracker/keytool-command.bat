keytool -genkeypair ^
  -alias springboot ^
  -keyalg RSA ^
  -keysize 2048 ^
  -validity 99999 ^
  -storetype PKCS12 ^
  -keystore src/main/resources/default-keystore.p12 ^
  -storepass changeit ^
  -dname "CN=localhost, OU=Dev, O=MyApp, L=Istanbul, C=TR"