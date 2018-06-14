import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Arrays;




class CountMinSketch implements Serializable{
    public static final long PRIME_MODULUS = (1L << 31) - 1;
    int depth;
    int width;
    long[][] table;
    long[] hashA;
    long size;
    double eps;
    double confidence;

    public CountMinSketch(int depth, int width, int seed) {
        this.depth = depth;
        this.width = width;
        this.eps = 2.0 / width;
        this.confidence = 1 - 1 / Math.pow(2, depth);
        initTablesWith(depth, width, seed);
    }

    public CountMinSketch() {

    }

    private void initTablesWith(int depth, int width, int seed) {
        this.table = new long[depth][width];
        this.hashA = new long[depth];
        Random r = new Random(seed);
        // We're using a linear hash functions
        // of the form (a*x+b) mod p.
        // a,b are chosen independently for each hash function.
        // However we can set b = 0 as all it does is shift the results
        // without compromising their uniformity or independence with
        // the other hashes.
        for (int i = 0; i < depth; ++i) {
            hashA[i] = r.nextInt(Integer.MAX_VALUE);
        }
    }


    int hash(long item, int i) {
        long hash = hashA[i] * item;
        // A super fast way of computing x mod 2^p-1
        // See http://www.cs.princeton.edu/courses/archive/fall09/cos521/Handouts/universalclasses.pdf
        // page 149, right after Proposition 7.
        hash += hash >> 32;
        hash &= PRIME_MODULUS;
        // Doing "%" after (int) conversion is ~2x faster than %'ing longs.
        return ((int) hash) % width;
    }
    private static void checkSizeAfterOperation(long previousSize, String operation, long newSize) {
        if (newSize < previousSize) {
           /* throw new IllegalStateException("Overflow error: the size after calling `" + operation +
                    "` is smaller than the previous size. " +
                    "Previous size: " + previousSize +
                    ", New size: " + newSize);
                */
            System.out.println("OverFlow error");
        }
    }

    private void checkSizeAfterAdd(String item, long count) {
        long previousSize = size;
        size += count;
        checkSizeAfterOperation(previousSize, "add(" + item + "," + count + ")", size);
    }

    public void add(long item, long count) {
        if (count < 0) {
            // Actually for negative increments we'll need to use the median
            // instead of minimum, and accuracy will suffer somewhat.
            // Probably makes sense to add an "allow negative increments"
            // parameter to constructor.
            //throw new IllegalArgumentException("Negative increments not implemented");
            System.out.println("Negative increments");
        }
        for (int i = 0; i < depth; ++i) {
            table[i][hash(item, i)] += count;
        }

        checkSizeAfterAdd(String.valueOf(item), count);
    }

    public long estimateCount(long item) {
        long res = Long.MAX_VALUE;
        for (int i = 0; i < depth; ++i) {
            res = Math.min(res, table[i][hash(item, i)]);
        }
        return res;
    }

    private static int[] getHashBuckets(String key,int hashCount,int max) {

        int[] result = new int[hashCount];
        int hash1 = key.hashCode();
        String k2 = key + "newHash";
        int hash2 = key.hashCode();

        for(int i = 0 ; i < hashCount ; i++) {
            result[i] = Math.abs((hash1 + i * hash2)%max);

        }

        return result;




    }

    public void add(String item, long count) {
        if (count < 0) {
            // Actually for negative increments we'll need to use the median
            // instead of minimum, and accuracy will suffer somewhat.
            // Probably makes sense to add an "allow negative increments"
            // parameter to constructor.
            throw new IllegalArgumentException("Negative increments not implemented");
        }
        int[] buckets = getHashBuckets(item, depth, width);
        for (int i = 0; i < depth; ++i) {
            table[i][buckets[i]] += count;
        }

        checkSizeAfterAdd(item, count);
    }

    public long estimateCount(String item) {
        long res = Long.MAX_VALUE;
        int[] buckets = getHashBuckets(item, depth, width);
        for (int i = 0; i < depth; ++i) {
            res = Math.min(res, table[i][buckets[i]]);
        }
        return res;
    }

    public void printTable() {
        for(int i = 0 ; i < depth ; i++)
        {
            System.out.println("Row "+i+" :  " );
            for(int j = 0 ; j < width ; j++)
            {
                System.out.print(table[i][j] + "    ");
            }
            System.out.println("\n");
        }
    }

    public static byte[] serialize(CountMinSketch sketch) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream s = new DataOutputStream(bos);
        try {
            s.writeLong(sketch.size);
            s.writeInt(sketch.depth);
            s.writeInt(sketch.width);
            for (int i = 0; i < sketch.depth; ++i) {
                s.writeLong(sketch.hashA[i]);
                for (int j = 0; j < sketch.width; ++j) {
                    s.writeLong(sketch.table[i][j]);
                }
            }
            return bos.toByteArray();
        } catch (IOException e) {
            // Shouldn't happen
            throw new RuntimeException(e);
        }
    }

    public static CountMinSketch deserialize(byte[] data) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream s = new DataInputStream(bis);
        try {
            CountMinSketch sketch = new CountMinSketch();
            sketch.size = s.readLong();
            sketch.depth = s.readInt();
            sketch.width = s.readInt();
            sketch.eps = 2.0 / sketch.width;
            sketch.confidence = 1 - 1 / Math.pow(2, sketch.depth);
            sketch.hashA = new long[sketch.depth];
            sketch.table = new long[sketch.depth][sketch.width];
            for (int i = 0; i < sketch.depth; ++i) {
                sketch.hashA[i] = s.readLong();
                for (int j = 0; j < sketch.width; ++j) {
                    sketch.table[i][j] = s.readLong();
                }
            }
            return sketch;
        } catch (IOException e) {
            // Shouldn't happen
            throw new RuntimeException(e);
        }
    }






}

public class Main {

    public static void main(String []args){
        /*System.out.println("Hello World");
        CountMinSketch s = new CountMinSketch(10,10,30);
        s.add(20,1);
        s.add(10,1);
        s.add(30,1);
        s.add(10,1);

        //s.printTable();
       // System.out.println(s.estimateCount(10));
        //System.out.println(s.estimateCount(20));

        byte[] b = s.serialize(s);
       /* for(int i = 0 ; i < b.length ; i++)
        {
            System.out.print(b[i]);
        }
        */
       /*
        try {
            FileOutputStream out = new FileOutputStream("MinCountObject.txt");
            out.write(b);
            out.close();

        }
        catch(IOException e)
        {
            System.out.println("Error in opening");
        }
        File f = new File("MinCountObject.txt");
        byte[] b1 = new byte[(int)f.length()];
        try {

            FileInputStream in = new FileInputStream("MinCountObject.txt");
            in.read(b1);



        }
        catch (IOException e)
        {
            System.out.println("Error reading ");
        }

        CountMinSketch s1 = new CountMinSketch();
        s1 = s1.deserialize(b1);

        s1.printTable();

        System.out.println(s1.estimateCount(10));

*/
  /*      String name = "haja";
        byte[] toCheckHash = new byte[10];
        try {
            toCheckHash = name.getBytes("UTF-16");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        int[] result = CountMinSketch.getHashBuckets(toCheckHash,4,10);
        for (int i = 0 ; i < result.length ; i++)
        {
            System.out.println("iteration "+i+" : "+result[i]);
        }
        /*
        System.out.println("New Testing");

        byte[] newBy = new byte[0];
        try {
            newBy = name.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        for(int i = 0 ; i < newBy.length ; i++)
        {
            System.out.println(newBy[i]);
        }

        System.out.println("Hii this is the hash" + MurmurHash.hash(newBy,newBy.length,10));
        */


        //System.out.println(name.codePointAt(0));
    //    System.out.println(45 >>> 3);
/*
        try {
            System.out.println(Arrays.toString(MessageDigest.getInstance("MD5").digest("test".getBytes())));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }



        System.out.println(Character.toCodePoint('h','a'));
        System.out.println(Character.isHighSurrogate('a'));

/*
        try {
            System.out.println(MurmurHash.hash(name.getBytes("UTF-8"),name.length(),10));
            byte[] by = name.getBytes("UTF-8");
            for( int i = 0 ; i < by.length ; i++)
            {
                System.out.println((int)by[i]);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        */
      /*  String a = "abcd-efgh-ijkl-mnop-qrst";
        //System.out.println(a.hashCode());

        long startTime = System.nanoTime();
        try {
           MessageDigest md =  MessageDigest.getInstance("MD5");
           md.update(a.getBytes());
           byte[] digest = md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        long endTime = System.nanoTime();

        System.out.println("Time for md5 :" );
        System.out.println(endTime-startTime);

        startTime = System.nanoTime();
        a.hashCode();
        endTime = System.nanoTime();

        System.out.println("Time for default hash ");
        System.out.println(endTime-startTime);


        String be = a + "Row_Number : 1";
        //System.out.println(Lookup3Hash.lookup3ycs(a,0,a.length(),0)%93);
        System.out.println(be.hashCode() % 97);
        be = a + "Row_Number : 2";
        System.out.println(be.hashCode() % 97);
        be = a + "Row_Number : 3";
        System.out.println(be.hashCode() % 97);
        */
        CountMinSketch s = new CountMinSketch(54,2000000,30);
        String[] Words = new String[17 * 1000000];
        Multiset<String> mset = HashMultiset.create();
        try {
            int i = 0;
            FileReader reader = new FileReader("Words1.txt");
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Words[i] = line;
                i++;
                //mset.add(line);


            }
            reader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(Words.length);
        System.out.println(mset.size());





    }
}
