package jp.azisaba.lgw.kdstatus.sql;

import jp.azisaba.lgw.kdstatus.KDStatusReloaded;
import jp.azisaba.lgw.kdstatus.utils.TimeUnit;
import jp.azisaba.lgw.kdstatus.utils.UUIDConverter;
import lombok.NonNull;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class PlayerDataMySQLController {

    KDStatusReloaded plugin;

    public PlayerDataMySQLController(KDStatusReloaded plugin){ this.plugin = plugin; }

    public void createTable(){

        PreparedStatement ps;
        try{

            ps = plugin.sql.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS killdeathdata "
                    + "(UUID VARCHAR(64) NOT NULL ,NAME VARCHAR(36) NOT NULL," +
                    "kills INT DEFAULT 0, " +
                    "deaths INT DEFAULT 0 ," +
                    "daily_kills INT DEFAULT 0," +
                    "monthly_kills INT DEFAULT 0," +
                    "yearly_kills INT DEFAULT 0," +
                    "last_updated BIGINT DEFAULT -1 )");

            ps.executeUpdate();

        }catch (SQLException e){e.printStackTrace();}

    }

    public boolean exist(UUID uuid){

        try {

            PreparedStatement ps = plugin.sql.getConnection().prepareStatement("SELECT * FROM killdeathdata WHERE NAME=?");
            ps.setString(1,uuid.toString());

            ResultSet result = ps.executeQuery();

            if(result.next()){
                return true;
            }

            return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;

    }

    public void create(KDUserData data){

        if(exist(data.getUuid()))
            return;

        try {

            PreparedStatement ps = plugin.sql.getConnection().prepareStatement("INSERT INTO killdeathdata (UUID,NAME,kills,deaths,daily_kills,monthly_kills,yearly_kills,last_updated) VALUES (?,?,?,?,?,?,?,?)");
            ps.setString(1,data.getUuid().toString());
            ps.setString(2,data.getName());
            ps.setInt(3,data.getKills(TimeUnit.LIFETIME));
            ps.setInt(4,data.getDeaths());
            ps.setInt(5,data.getKills(TimeUnit.DAILY));
            ps.setInt(6,data.getKills(TimeUnit.MONTHLY));
            ps.setInt(7,data.getKills(TimeUnit.YEARLY));
            ps.setLong(8,data.getLastUpdated());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public boolean update(KDUserData data){

        try {

            PreparedStatement ps = plugin.sql.getConnection().prepareStatement("UPDATE killdeathdata SET NAME=? ,kills=? ,deaths=? ,daily_kills=? ,monthly_kills=? ,yearly_kills=? ,last_updated=? WHERE UUID=?");
            ps.setString(8,data.getUuid().toString());
            ps.setString(1,data.getName());
            ps.setInt(2,data.getKills(TimeUnit.LIFETIME));
            ps.setInt(3,data.getDeaths());
            ps.setInt(4,data.getKills(TimeUnit.DAILY));
            ps.setInt(5,data.getKills(TimeUnit.MONTHLY));
            ps.setInt(6,data.getKills(TimeUnit.YEARLY));
            ps.setLong(7,data.getLastUpdated());

            ps.executeUpdate();
            ps.close();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

    }

    public BigInteger getKills(@NonNull UUID uuid, @NonNull TimeUnit unit) {

        try {

            PreparedStatement ps = plugin.sql.getConnection().prepareStatement("SELECT "+ unit.getSqlColumnName() + " FROM killdeathdata WHERE UUID=?");
            ps.setString(1, uuid.toString());

            ResultSet result = ps.executeQuery();

            if(result.next()){
                return BigInteger.valueOf(result.getInt(1));
            }

            ps.close();

            return BigInteger.valueOf(-1);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return BigInteger.valueOf(-1);

    }

    public BigInteger getDeaths(@NonNull UUID uuid) {

        try {

            PreparedStatement ps = plugin.sql.getConnection().prepareStatement("SELECT death FROM killdeathdata WHERE UUID=?");
            ps.setString(1, uuid.toString());

            ResultSet result = ps.executeQuery();

            if(result.next()){
                return BigInteger.valueOf(result.getInt(1));
            }

            ps.close();

            return BigInteger.valueOf(-1);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return BigInteger.valueOf(-1);

    }

    public String getName(@NonNull UUID uuid) {

        try {

            PreparedStatement ps = plugin.sql.getConnection().prepareStatement("SELECT NAME FROM killdeathdata WHERE UUID=?");
            ps.setString(1, uuid.toString());

            ResultSet result = ps.executeQuery();

            if(result.next()){
                return result.getString(1);
            }

            ps.close();

            return null;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;

    }

    public long getLastUpdated(@NonNull UUID uuid) {

        try {

            PreparedStatement ps = plugin.sql.getConnection().prepareStatement("SELECT last_updated FROM killdeathdata WHERE UUID=?");
            ps.setString(1, uuid.toString());

            ResultSet result = ps.executeQuery();

            if(result.next()){
                return result.getLong(1);
            }

            ps.close();

            return -1;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;

    }

    public ResultSet getRawData(@NonNull UUID uuid) {

        PreparedStatement ps;
        try {
            ps = plugin.sql.getConnection().prepareStatement("SELECT * FROM killdeathdata WHERE UUID=?");
            ps.setString(1, uuid.toString());
            return ps.executeQuery();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    public int getRank(UUID uuid,TimeUnit unit){

        try {
            PreparedStatement ps = plugin.sql.getConnection().prepareStatement("select uuid, name, kills, (SELECT count(*) FROM killdeathdata"
                                            + " as p1 WHERE p1." + unit.getSqlColumnName() + " > p." + unit.getSqlColumnName()
                                            + ") + 1 as rank FROM killdeathdata as p"
                                            + " where uuid=? and last_updated > " + getFirstMilliSecond(unit) + " order by rank");
            ps.setString(1, uuid.toString());

            ResultSet result = ps.executeQuery();

            if(result.next()){
                return result.getInt("rank");
            }
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return -1;

    }

    public List<KillRankingData> getTopKillRankingData(TimeUnit unit, int count){

        try {
            /*
            PreparedStatement ps = plugin.sql.getConnection().prepareStatement("select uuid, name, " + unit.getSqlColumnName()
                    + " from killdeathdata"
                    + " where last_updated >= " + getFirstMilliSecond(unit)
                    + " order by " + unit.getSqlColumnName() + " DESC"
                    + " LIMIT " + count);

             */

            //最新機種
            PreparedStatement ps5 = plugin.sql.getConnection().prepareStatement("SELECT uuid, name, ? from killdeathdata" +
                    " where last_updated >= ? order by ? DESC LIMIT ?");

            ps5.setString(1,unit.getSqlColumnName());
            ps5.setLong(2,getFirstMilliSecond(unit));
            ps5.setString(3,unit.getSqlColumnName());
            ps5.setInt(4,count);

            List<KillRankingData> ranking = new ArrayList<>();

            ResultSet result = ps5.executeQuery();

            while (result.next()){

                UUID uuid = UUID.fromString(result.getString("uuid"));
                String mcid = result.getString("name");
                int kills = result.getInt(unit.getSqlColumnName());

                // プレイヤーデータの作成
                KillRankingData data = new KillRankingData(uuid, mcid, kills);

                // リストに追加
                ranking.add(data);

            }

            ps5.close();

            return ranking;

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return null;

    }

    private long getFirstMilliSecond(TimeUnit unit) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);

        if ( unit == TimeUnit.DAILY )
            return cal.getTimeInMillis();

        cal.set(Calendar.DATE, 1);

        if ( unit == TimeUnit.MONTHLY )
            return cal.getTimeInMillis();

        cal.set(Calendar.MONTH, 0);

        if ( unit == TimeUnit.YEARLY )
            return cal.getTimeInMillis();

        return -1;
    }

}
