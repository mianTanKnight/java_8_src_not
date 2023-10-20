package test;

import java.util.HashMap;

/**
 * hashMap的源码分析
 * @author admin
 */
public class HashMapSrc_ {


    public static void main(String[] args) {

        HashMap<Object, Object> hashMap = new HashMap<>(2);

        for (int i = 0; i < 4; i++) {
            hashMap.put(i,i+1);
        }

    }

}
