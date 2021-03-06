package com.cyanspring.server.persistence;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.cyanspring.common.account.TerminationStatus;
import com.cyanspring.common.account.ThirdPartyUser;
import com.cyanspring.common.account.UserLoginType;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cyanspring.common.Default;
import com.cyanspring.common.account.User;
import com.cyanspring.common.account.UserType;

// TODO: JDBC Template
public class CentralDbConnector {

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static String insertUser = "INSERT INTO AUTH(`USERID`, `USERNAME`, `PASSWORD`, `SALT`, `EMAIL`, `PHONE`, `CREATED`, `USERTYPE`, `COUNTRY`, `LANGUAGE`, `USERLEVEL`) VALUES('%s', '%s', md5('%s'), '%s', '%s', '%s', '%s', %d, '%s', '%s', %d)";
	private static String insertThirdPartyUser = "INSERT INTO THIRD_PARTY_USER(`ID`, `MARKET`, `LANGUAGE`, `USERID`, `USERTYPE`) VALUES('%s', '%s', '%s', '%s', '%s')";
	private static String isUserExist = "SELECT COUNT(*) FROM AUTH WHERE `USERID` = '%s'";
	private static String isUserExistAndNotTerminated = "SELECT COUNT(*) FROM AUTH WHERE `USERID` = '%s' AND (ISTERMINATED=0 OR ISTERMINATED is null)";
    private static String isThirdPartyUserExist = "SELECT COUNT(*) FROM THIRD_PARTY_USER WHERE `ID` = '%s' AND `MARKET` = '%s' AND `LANGUAGE` = '%s'";
	private static String isThirdPartyUserAnyMappingExist = "SELECT COUNT(*) FROM THIRD_PARTY_USER WHERE `ID` = '%s'";
	private static String isPendingTransfer = "SELECT COUNT(*) FROM ToDoCvtFDT WHERE `ID3RD` = '%s' AND `STATUS` IS NULL";
    private static String insertPendingTransfer = "INSERT INTO ToDoCvtFDT(`ID3RD`, `USERID`) VALUES('%s', '%s')";
    private static String getUserIdFromThirdPartyId = "SELECT `USERID` FROM THIRD_PARTY_USER WHERE `ID` = '%s' AND `MARKET` = '%s' AND `LANGUAGE` = '%s'";
    private static String getUserThirdPartyIds = "SELECT `ID`, `USERTYPE` FROM THIRD_PARTY_USER WHERE `USERID` = '%s' AND `MARKET` = '%s' AND `LANGUAGE` = '%s'";
    private static String detachThirdPartyUser = "DELETE FROM THIRD_PARTY_USER WHERE `ID` = '%s' AND `USERID` = '%s' AND `MARKET` = '%s' AND `LANGUAGE` = '%s'";
    private static String deleteSameTypeThirdPartyUser = "DELETE FROM THIRD_PARTY_USER WHERE `USERID` = '%s' AND `USERTYPE` = '%s' AND `MARKET` = '%s' AND `LANGUAGE` = '%s'";
	private static String isEmailExist = "SELECT COUNT(*) FROM AUTH WHERE `EMAIL` = '%s'";
	private static String isPhoneExist = "SELECT COUNT(*) FROM AUTH WHERE `PHONE` = '%s'";
	private static String getUserPasswordSalt = "SELECT `PASSWORD`, `SALT` FROM AUTH WHERE `USERID` = '%s'";
	private static String getUserAllInfo = "SELECT `USERID`, `USERNAME`, `PASSWORD`, `SALT`, `EMAIL`, `PHONE`, `CREATED`, `USERTYPE`, `COUNTRY`, `LANGUAGE`, `USERLEVEL`, `ISTERMINATED` FROM AUTH WHERE `USERID` = '%s'";
    private static String getUserAllInfoByEmail = "SELECT `USERID`, `USERNAME`, `PASSWORD`, `SALT`, `EMAIL`, `PHONE`, `CREATED`, `USERTYPE`, `COUNTRY`, `LANGUAGE`, `USERLEVEL`, `ISTERMINATED` FROM AUTH WHERE `EMAIL` = '%s' LIMIT 1";
    private static String getUserAllInfoByPhone = "SELECT `USERID`, `USERNAME`, `PASSWORD`, `SALT`, `EMAIL`, `PHONE`, `CREATED`, `USERTYPE`, `COUNTRY`, `LANGUAGE`, `USERLEVEL`, `ISTERMINATED` FROM AUTH WHERE `PHONE` = '%s' LIMIT 1";
	private static String setUserPassword = "UPDATE AUTH SET `PASSWORD` = '%s' WHERE `USERID` = '%s'";
	private static String setUserTermination = "UPDATE AUTH SET `ISTERMINATED` =  '%s' WHERE `USERID` = '%s'";
	private static final Logger log = LoggerFactory.getLogger(CentralDbConnector.class);
	private ComboPooledDataSource cpds;	

	public void setCpds(ComboPooledDataSource cpds) {
		this.cpds = cpds;
	}

	private CentralDbConnector() {

	}

	public Connection connect() {
		try {
			Connection conn = cpds.getConnection();

			log.info("Connected to the database");

            return conn;
		} catch (Exception e) {
			log.error("Cannot connection to Database", e);
			return null;
		}
	}

	protected String getInsertUserSQL(String userId, String userName,
			String password, String salt, String email, String phone, Date created,
			UserType userType, String country, String language) {
		return String.format(insertUser, userId, userName, password + salt, salt, email,
                phone, sdf.format(created), userType.getCode(), country, language, 0);
	}

    /**
     *
     * @param id
     *          third party ID
     * @param userId
     * @param userType
     * @return
     */
	protected String getInsertThirdPartyUserSQL(String id, String market, String language, String userId, UserType userType) {
		return String.format(insertThirdPartyUser, id, market, language, userId, userType.getCode());
	}

    public boolean registerUser(
            String userId, String userName, String password, String email, String phone, UserType userType,
            String country, String language) {
        return registerUser(userId, userName, password, email, phone, userType, country, language, null, null);
    }


    public boolean registerUser(
            String userId, String userName, String password, String email, String phone, UserType userType,
            String country, String language, String thirdPartyId, String market) {

        if (email != null)
            email = email.trim().toLowerCase();
        if (phone != null)
            phone = phone.trim().toLowerCase();

        Connection conn = connect();

		if (null == conn)
			return false;

		boolean bIsSuccess = false;
		Date now = Default.getCalendar().getTime();
		String salt = getRandomSalt(10);

        UserType authUserType = userType;

        if (userType.isThirdParty() && !Strings.isNullOrEmpty(thirdPartyId) && !Strings.isNullOrEmpty(market)) {
            authUserType = UserType.NORMAL;
        }

		String sUserSQL = getInsertUserSQL(userId, userName, password, salt, email, phone, now, authUserType, country, language);
		Statement stmt = null;
		log.debug("[registerUser] SQL:" + sUserSQL);
		try {
        	conn.setAutoCommit(false);
			stmt = conn.createStatement();
			stmt.executeUpdate(sUserSQL);

			if (userType.isThirdParty() && !Strings.isNullOrEmpty(thirdPartyId) && !Strings.isNullOrEmpty(market)) {
				String sql = getInsertThirdPartyUserSQL(thirdPartyId.toLowerCase(), market, language, userId, userType);
                stmt.executeUpdate(sql);
			}

            // Add to transfer table and terminate FDT account for transferring.
            if (!Strings.isNullOrEmpty(thirdPartyId) && isUserExist(thirdPartyId.toLowerCase())) {

                String sql = String.format(insertPendingTransfer, thirdPartyId.toLowerCase(), userId);
                stmt.executeUpdate(sql);

                sql = String.format(setUserTermination, TerminationStatus.TRANSFERRING.getValue(), userId);
                stmt.executeUpdate(sql);
            }

            bIsSuccess = true;
			conn.commit();
		} catch (SQLException e) {
			bIsSuccess = false;
			log.warn("Cannot register user."+userId, e);
			try {
				conn.rollback();
			} catch (SQLException se) {
				log.warn("Register User rollback fail."+userId, se);
			}
		} finally {
			if (stmt != null) {
				closeStmt(stmt);
			}
            if (conn != null) {
                closeConn(conn);
            }
		}
		return bIsSuccess;
	}

    public boolean detachThirdPartyUser(String userId, String password, String thirdPartyId, String market, String language) {

        if (!Strings.isNullOrEmpty(password) && !userLogin(userId, password))
            return false;

        Connection conn = connect();

        if (null == conn)
            return false;

        boolean bIsSuccess = false;
        Statement stmt = null;

        try {
            conn.setAutoCommit(false);
            stmt = conn.createStatement();

            String sql = String.format(detachThirdPartyUser, thirdPartyId, userId, market, language);
            stmt.executeUpdate(sql);

            bIsSuccess = true;
            conn.commit();

        } catch (SQLException e) {
            bIsSuccess = false;
            log.warn("Cannot detach third party user." + userId + ", " + thirdPartyId, e);
            try {
                conn.rollback();
            } catch (SQLException se) {
                log.warn("Detach third party user rollback fail." + userId + ", " + thirdPartyId, se);
            }
        } finally {
            if (stmt != null) {
                closeStmt(stmt);
            }
            if (conn != null) {
                closeConn(conn);
            }
        }

        return bIsSuccess;
    }

    public boolean registerThirdPartyUser(String userId, UserType userType, String thirdPartyId, String market, String language) {

        Connection conn = connect();

        if (null == conn)
            return false;

        boolean bIsSuccess = false;
        Statement stmt = null;

        try {
            conn.setAutoCommit(false);
            stmt = conn.createStatement();

            String sql = String.format(deleteSameTypeThirdPartyUser, userId, userType.getCode(), market, language);
            stmt.executeUpdate(sql);

            sql = getInsertThirdPartyUserSQL(thirdPartyId.toLowerCase(), market, language, userId, userType);
            stmt.executeUpdate(sql);

            bIsSuccess = true;
            conn.commit();
        } catch (SQLException e) {
            bIsSuccess = false;
            log.warn("Cannot register third party user." + userId + ", " + thirdPartyId, e);
            try {
                conn.rollback();
            } catch (SQLException se) {
                log.warn("Register third party user rollback fail." + userId + ", " + thirdPartyId, se);
            }
        } finally {
            if (stmt != null) {
                closeStmt(stmt);
            }
            if (conn != null) {
                closeConn(conn);
            }
        }
        return bIsSuccess;
    }

	public boolean isUserExist(String sUser) {

        Connection conn = connect();

		if (null == conn)
            return false;

        String sQuery = String.format(isUserExist, sUser);
		log.debug("[isUserExist] SQL:" + sQuery);
		Statement stmt = null;
		int nCount = 0;

		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sQuery);

			if (rs.next())
				nCount = rs.getInt("COUNT(*)");

		} catch (SQLException e) {
			log.warn(e.getMessage(), e);
		} finally {
			if (stmt != null) {
				closeStmt(stmt);
			}
            if (conn != null) {
                closeConn(conn);
            }
		}
		return (nCount > 0);
	}

    public boolean isUserExistAndNotTerminated(String sUser) {

        Connection conn = connect();

        if (null == conn)
            return false;

        String sQuery = String.format(isUserExistAndNotTerminated, sUser);
        log.debug("[isUserExistAndNotTerminated] SQL:" + sQuery);
        Statement stmt = null;
        int nCount = 0;

        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sQuery);

            if (rs.next())
                nCount = rs.getInt("COUNT(*)");

        } catch (SQLException e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (stmt != null) {
                closeStmt(stmt);
            }
            if (conn != null) {
                closeConn(conn);
            }
        }
        return (nCount > 0);
    }

    public boolean isThirdPartyUserExist(String id, String market, String language) {

        Connection conn = connect();

        if (null == conn)
            return false;

        String sQuery = String.format(isThirdPartyUserExist, id, market, language);
        log.debug("[isThirdPartyUserExist] SQL:" + sQuery);
        Statement stmt = null;
        int nCount = 0;

        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sQuery);

            if (rs.next())
                nCount = rs.getInt("COUNT(*)");

        } catch (SQLException e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (stmt != null) {
                closeStmt(stmt);
            }
            if (conn != null) {
                closeConn(conn);
            }
        }
        return (nCount > 0);
    }

	public boolean isThirdPartyUserAnyMappingExist(String id) {

		Connection conn = connect();

		if (null == conn)
			return false;

		String sQuery = String.format(isThirdPartyUserAnyMappingExist, id);
		log.debug("[isThirdPartyUserAnyMappingExist] SQL:" + sQuery);
		Statement stmt = null;
		int nCount = 0;

		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sQuery);

			if (rs.next())
				nCount = rs.getInt("COUNT(*)");

		} catch (SQLException e) {
			log.warn(e.getMessage(), e);
		} finally {
			if (stmt != null) {
				closeStmt(stmt);
			}
			if (conn != null) {
				closeConn(conn);
			}
		}
		return (nCount > 0);
	}

    public boolean isThirdPartyUserPendingTransfer(String thirdPartyId) {

        Connection conn = connect();

        String sql = String.format(isPendingTransfer, thirdPartyId);
        log.debug("[isThirdPartyUserPendingTransfer] SQL:" + sql);

        Statement stmt = null;
        int nCount = 0;

        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next())
                nCount = rs.getInt("COUNT(*)");

        } catch (SQLException e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (stmt != null) {
                closeStmt(stmt);
            }
            if (conn != null) {
                closeConn(conn);
            }
        }

        return (nCount > 0);
    }
	
	public boolean isEmailExist(String sEmail) {

        Connection conn = connect();

		if (null == conn)
			return false;

		String sQuery = String.format(isEmailExist, sEmail);
		log.debug("[isEmailExist] SQL:" + sQuery);
		Statement stmt = null;
		int nCount = 0;

		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sQuery);

			if (rs.next())
				nCount = rs.getInt("COUNT(*)");

		} catch (SQLException e) {
			log.warn(e.getMessage(), e);
		} finally {
			if (stmt != null) {
				closeStmt(stmt);
			}
            if (conn != null) {
                closeConn(conn);
            }
		}
		return (nCount > 0);
	}

    public boolean isPhoneExist(String phone) {

        Connection conn = connect();

        if (null == conn)
            return false;

        String sQuery = String.format(isPhoneExist, phone);
        log.debug("[isPhoneExist] SQL:" + sQuery);
        Statement stmt = null;
        int nCount = 0;

        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sQuery);

            if (rs.next())
                nCount = rs.getInt("COUNT(*)");

        } catch (SQLException e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (stmt != null) {
                closeStmt(stmt);
            }
            if (conn != null) {
                closeConn(conn);
            }
        }

        return (nCount > 0);
    }

	public boolean userLogin(String sUser, String sPassword) {
		return userLoginEx(sUser, sPassword) != null;
	}

    public User userLoginEx(String sUser, String sPassword) {
        return userLoginEx(sUser, sPassword, UserLoginType.USER_ID);
    }

	public User userLoginEx(String sUser, String sPassword, UserLoginType loginType) {

        Connection conn = connect();

		if (null == conn) {
			log.debug("[userLoginEx] Connection is lost ,could not process userLogin :"+sUser);
			return null;
		}

		String sQuery;

        switch (loginType) {
            case EMAIL:
                sQuery = String.format(getUserAllInfoByEmail, sUser);
                break;
            case PHONE:
                sQuery = String.format(getUserAllInfoByPhone, sUser);
                break;
            case USER_ID:
            default:
                sQuery = String.format(getUserAllInfo, sUser);
                break;
        }

		log.debug("[userLoginEx] SQL:" + sQuery);
		Statement stmt = null;

		String md5Password = null;
		String salt = null;
		String username = null;
		String email = null;
		String phone = null;
		UserType userType = null;
		int isTerminated = 0;

		try {
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sQuery);

			if (rs.next()){
                sUser = rs.getString("USERID"); // It may be email or phone, change back to user id.
				md5Password = rs.getString("PASSWORD");
				salt = rs.getString("SALT");
				username = rs.getString("USERNAME");
				email = rs.getString("EMAIL");
				phone = rs.getString("PHONE");
				userType = UserType.fromCode(rs.getInt("USERTYPE"));
				isTerminated = rs.getInt("ISTERMINATED");
			}
			log.debug(String.format("[userLoginEx] user[%s] queried: PASSWROD[%s] SALT[%s] USERNAME[%s] EMAIL[%s] PHONE[%s] USERTYPE[%s] ISTERMINATED[%s]",
					sUser == null ? "null" : sUser,
					md5Password == null ? "null": md5Password,
					salt == null ? "null": salt,
					username == null ? "null": username,
					email == null ? "null": email,
					phone == null ? "null": phone,
					userType == null ? "null": userType.name(),
					isTerminated));
			
			if(md5Password == null){
				closeStmt(stmt);
				log.debug("[userLoginEx] db lost md5Password for user:" + sUser);
				return null;
			}

			String fullPassword = (salt == null)? sPassword : sPassword + salt;
			log.debug(String.format("[userLoginEx] user[%s] md5(fullPassword)[%s] fullPassword[%s] sPassword[%s] salt[%s] md5Password[%s]",
                    sUser == null ? "null" : sUser,
                    fullPassword == null ? "null" : md5(fullPassword),
                    fullPassword == null ? "null" : fullPassword,
                    sPassword == null ? "null" : sPassword,
                    salt == null ? "null" : salt,
                    md5Password == null ? "null" : md5Password));
			
			if(md5Password.equals(md5(fullPassword))){
				closeStmt(stmt);
				log.debug("[userLoginEx] password OK :" + sUser);
				return new User(sUser, username, sPassword, email, phone, userType, TerminationStatus.fromInt(isTerminated));
			}

		} catch (SQLException e) {
			log.warn(e.getMessage(), e);
		} finally {
			if (stmt != null) {
				closeStmt(stmt);
			}
            if (conn != null) {
                closeConn(conn);
            }
		}
		log.debug("[userLoginEx] password not match :" + sUser);
		return null;
	}

    public User getUser(String sUser) {

        Connection conn = connect();

        if (null == conn) {
            log.debug("[getUser] Connection is lost ,could not process userLogin :" + sUser);
            return null;
        }

        String sQuery = String.format(getUserAllInfo, sUser);
        log.debug("[getUser] SQL:" + sQuery);
        Statement stmt = null;

        String username = null;
        String email = null;
        String phone = null;
        UserType userType = null;
        int isTerminated = 0;

        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sQuery);

            if (rs.next()){
                username = rs.getString("USERNAME");
                email = rs.getString("EMAIL");
                phone = rs.getString("PHONE");
                userType = UserType.fromCode(rs.getInt("USERTYPE"));
                isTerminated = rs.getInt("ISTERMINATED");

                log.debug(String.format("[getUser] user[%s] queried: USERNAME[%s] EMAIL[%s] PHONE[%s] USERTYPE[%s] ISTERMINATED[%s]",
                        sUser == null ? "null" : sUser,
                        username == null ? "null": username,
                        email == null ? "null": email,
                        phone == null ? "null": phone,
                        userType == null ? "null": userType.name(),
                        isTerminated));

                return new User(sUser, username, "", email, phone, userType, TerminationStatus.fromInt(isTerminated));
            }

        } catch (SQLException e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (stmt != null) {
                closeStmt(stmt);
            }
            if (conn != null) {
                closeConn(conn);
            }
        }

        return null;
    }

    public String getUserIdFromThirdPartyId(String thirdPartyId, String market, String language) {

        Connection conn = connect();

        if (null == conn) {
            return null;
        }

        Statement stmt = null;

        try {
            String sQuery = String.format(getUserIdFromThirdPartyId, thirdPartyId, market, language);
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sQuery);

			if (rs.next()) {
				return rs.getString("USERID");
			} else {
				return null;
			}

        } catch (SQLException e) {

            closeStmt(stmt);
            log.warn(e.getMessage(), e);
            return null;

        } finally {
            if (stmt != null) {
                closeStmt(stmt);
            }
            if (conn != null) {
                closeConn(conn);
            }
        }
    }

    public List<ThirdPartyUser> getThirdPartyUsers(String userId, String market, String language) {

        Connection conn = connect();

        if (null == conn) {
            return ImmutableList.of();
        }

        Statement stmt = null;

        try {
            String sql = String.format(getUserThirdPartyIds, userId, market, language);
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            List<ThirdPartyUser> result = new ArrayList<>();

            while (rs.next()) {
                result.add(new ThirdPartyUser(rs.getString("ID"), UserType.fromCode(rs.getInt("USERTYPE"))));
            }

            return result;

        } catch (SQLException e) {

            closeStmt(stmt);
            log.warn(e.getMessage(), e);
            return ImmutableList.of();

        } finally {
            if (stmt != null) {
                closeStmt(stmt);
            }
            if (conn != null) {
                closeConn(conn);
            }
        }
    }

	public boolean changePassword(String sUser, String originalPass, String newPass) {

        Connection conn = connect();

		if (null == conn)
			return false;

		String sQuery = String.format(getUserPasswordSalt, sUser);
		log.debug("[changePassword] SQL:" + sQuery);
		Statement stmt = null;

		String md5Password = null;
		String salt = null;

		try {
        	conn.setAutoCommit(false);
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sQuery);

			if (rs.next())
			{
				md5Password = rs.getString("PASSWORD");
				salt = rs.getString("SALT");
			}
			
			if(md5Password == null){
				closeStmt(stmt);				
				return false;
			}

			String fullPassword = (salt == null)? originalPass : originalPass + salt;
			
			if(!md5Password.equals(md5(fullPassword))){
				closeStmt(stmt);				
				return false;
			}
			
			String newMd5Password = md5(newPass + salt);
			String sQuerySet = String.format(setUserPassword, newMd5Password, sUser);
			
			int nResult = stmt.executeUpdate(sQuerySet);
			if(1 != nResult) {
				closeStmt(stmt);
				return false;
			}
			conn.commit();
		} catch (SQLException e) {
			closeStmt(stmt);
			log.warn(e.getMessage(), e);
			return false;
		} finally {
			if (stmt != null) {
				closeStmt(stmt);
			}
            if (conn != null) {
                closeConn(conn);
            }
		}
		return true;
	}

	public boolean changeTermination(String userId, TerminationStatus terminationStatus) {

        Connection conn = connect();

		if (null == conn) {
			return false;
		}

		Statement stmt = null;

		try {
			conn.setAutoCommit(false);
			stmt = conn.createStatement();

			String sql = String.format(setUserTermination, terminationStatus.getValue(), userId);

			int result = stmt.executeUpdate(sql);
			if (1 != result) {
				closeStmt(stmt);
				return false;
			}

			conn.commit();

		} catch (SQLException e) {
			closeStmt(stmt);
			log.warn(e.getMessage(), e);
			return false;

		} finally {
			if (stmt != null) {
				closeStmt(stmt);
			}
            if (conn != null) {
                closeConn(conn);
            }
		}

		return true;
	}

	public boolean updateConnection(){

        Connection conn = connect();

		if (null == conn)
			return false;

		Statement stmt = null;
		boolean state = true;
		try{
			stmt = conn.createStatement();
			stmt.executeQuery("SELECT 1;");
		}catch(SQLException e){
			state = false;
			log.warn(e.getMessage(), e);
		}finally{
			if(stmt != null)
				closeStmt(stmt);
            if (conn != null)
                closeConn(conn);
		}
		return state;
	}
	
	private void closeStmt(Statement stmt) {
		try {
			stmt.close();
		} catch (SQLException e) {
			log.warn("Cannot close statement", e);
		}
	}

    private void closeConn(Connection conn) {
        try {
            conn.close();
        } catch (SQLException e) {
            log.warn("Cannot close connection", e);
        }
    }
	
	 protected String md5(String str) 
	 {
	    String md5 = null;
	    
	    try 
	    {
	      MessageDigest md=MessageDigest.getInstance("MD5");
	      byte[] barr=md.digest(str.getBytes());
	      StringBuffer sb=new StringBuffer();
	      
	      for (int i=0; i < barr.length; i++) 
	    	  sb.append(byte2Hex(barr[i]));
	      
	      return sb.toString();
	    }
	    catch(Exception e) 
	    {
	    	log.warn("create md5 error", e);
	    }
	    return md5;
	 }
	 
	 protected String byte2Hex(byte b) 
	 {
	    String[] h={"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
	    int i=b;
	    
	    if (i < 0) 
	    	{i += 256;}
	    
	    return h[i/16] + h[i%16];
	 }
		  
	 protected String getRandomSalt(int nLength)
	 {
		  int[] word = new int[nLength];
		  int mod;
		  for(int i = 0; i < nLength; i++)
		  {
			  mod = (int)((Math.random()*7)%3);
			  if(mod ==1)
				  word[i]=(int)((Math.random()*10) + 48);
			  else if(mod ==2)
				  word[i] = (char)((Math.random()*26) + 65);
			  else
				  word[i] = (char)((Math.random()*26) + 97);
		  }
		  
		  StringBuilder sKey = new StringBuilder();
		  for(int j=0 ; j<nLength ; j++)
			  sKey.append((char)word[j]);
		  
		  return sKey.toString();
	 }

	public static void main(String[] argv) {
//		CentralDbConnector conn = new CentralDbConnector();
//		boolean bConnect = conn.connect("125.227.191.247", 3306, "tqt001",
//				"tqt001", "LTS");
//		if (bConnect) {
//			conn.registerUser("aaaaaaa", "aaaaaaa", "aaaaaaa", "test1@test.com", "+886-12345678", UserType.NORMAL, "TW", "ZH");
//			//boolean bExist = conn.isUserExist("test1");
//			boolean bExist = conn.isEmailExist("phoenix.su@hkfdt.com");
//			boolean bLogin = conn.userLogin("seemo1", "1234");
//			//boolean bChangePassword = conn.changePassword("seemo1", "1111", "1234");
//			System.out.println("");
//		}
	}
}
