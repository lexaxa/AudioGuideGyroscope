package ru.alexis.audioguide.fortest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TestMain {

    private static final double RADIUS = 6371000;



    public static String s;
    public static int count = 0;

    public static int decode(String s) {
        boolean find = false;

        int addFirst = 0;
        TestMain.s = s;
        check(0);
        return count;
    }

    public static boolean check(int stpos){

        for(int i=stpos; i<s.length(); i++) {
            int check = Integer.parseInt(s.substring(i, i));
            if (check >= 3 && check <= 9) {
                check(i);
            } else{
                if(i+1<=s.length()) {
                    if(!isInRange(Integer.parseInt(s.substring(i, i + 1)))){
                        return false;
                    }
                }else {
                    check(i+1);
                    return true;
                }
            }
        }

        return true;
    }

    public static boolean isInRange(int code){
        return code>0 && code<=26;
    }

    public static void main(String[] args) {

//        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        String s1 = "2018-11-07T09:18:15.962Z";
        String s2 = "2018-05-09T07:48:03+00:00";
        Calendar c = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        try {
            Date moment = dateFormat2.parse(s2);
            c.setTime(moment);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if(c.after(today)) {
            System.out.println("time is " + String.format("%1$tH:%1$tM:%1$tS",c));
        }else{
            System.out.println("date is " + String.format("%1$te.%1$tm.%1$tY",c));
        }


        String a="1";
        int p=(int) a.charAt(0);

        double loc1Lat = 59.956995;
        double loc1Lon = 30.341660;

//        double loc2Lat = 59.928809; //vo
//        double loc2Lon = 30.347128;

        double loc2Lat = 59.984182; //lesnoy
        double loc2Lon = 30.344974;


        double metres = getMetres(loc1Lat, loc1Lon, loc2Lat, loc2Lon);

        double degree = 0; //Math.toDegrees(Math.atan(loc2.bearingTo(loc1)));
        double degree2= Math.atan(
                (loc2Lat-loc1Lat)/(loc2Lon - loc1Lon)
                )                *180/Math.PI;

        double x = 0;
        double y = 0;
        double z = 0;

//        double metres2 = Math.sqrt(Math.abs(x*x+y*y+z*z));
        double metres2 = getMetresCloserPoints(loc1Lat, loc1Lon, loc2Lat, loc2Lon);

        //azim = atan2 (North2-North1, East2-East1)

        System.out.println(Math.round(degree) + "' " + Math.round(degree2) + "' " + metres + "m");
        System.out.println(Math.round(degree) + "' " + Math.round(degree2) + "' " + metres2 + "m");

        System.out.printf("%.6f",loc1Lat);
        System.out.printf("%-20.15s%n","Math.ro");
        System.out.printf("%-20.15s%n","Math.round(degree) + asdf awef");
        System.out.printf("%-20.15s%n","Math.ro");
        System.out.printf("%-20.15s%n","Math.round(degree) + asdf awef");


    }
    public static double getMetresCloserPoints(double latitude1, double longitude1, double latitude2, double longitude2){
        long start = System.currentTimeMillis();
        double φ1 = Math.toRadians(latitude1), φ2 = Math.toRadians(latitude2), Δλ = Math.toRadians(longitude2-longitude1);
        double λ1 = Math.toRadians(longitude1), λ2 = Math.toRadians(longitude2);
        //var d = Math.acos( Math.sin(φ1)*Math.sin(φ2) + Math.cos(φ1)*Math.cos(φ2) * Math.cos(Δλ) ) * R;

        double x = (λ2-λ1) * Math.cos((φ1+φ2)/2);
        double y = (φ2-φ1);
        double d = Math.sqrt(x*x + y*y) * RADIUS;
        System.out.println(System.currentTimeMillis() - start);
        return d;

    }
    public static double getMetres(double latitude1, double longitude1, double latitude2, double longitude2){
        long start = System.currentTimeMillis();
        int R = 6371000; // metres
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        double dLat = Math.toRadians(latitude2 - latitude1);
        double dLon = Math.toRadians(longitude2 - longitude1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        double d = R * c;
        System.out.println(System.currentTimeMillis() - start);
        return  Math.round(d);
    }
}
