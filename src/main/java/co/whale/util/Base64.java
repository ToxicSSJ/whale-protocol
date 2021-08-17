package co.whale.util;

import java.io.*;

public class Base64 {

    public static Object fromString( String s ) throws IOException,
            ClassNotFoundException {
        byte [] data = java.util.Base64.getDecoder().decode( s );
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(  data ) );
        Object o  = ois.readObject();
        ois.close();
        return o;
    }

    public static String toString( Serializable o ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( o );
        oos.close();
        return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
    }

}
