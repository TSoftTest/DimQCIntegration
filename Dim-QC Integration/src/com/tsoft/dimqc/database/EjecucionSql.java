package com.tsoft.dimqc.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EjecucionSql {

	public static List<Map<String, Object>> executeQuery(String query, Connection conn) throws SQLException {

		ResultSet result = null;
		Statement stmt = null;
		try {
			if (conn != null) {
				stmt = conn.createStatement();
				if (stmt != null) {
					result = stmt.executeQuery(query);
					return resultSetToArrayList(result);
				}
			}
			return null;

		} catch (SQLException e) {
			throw e;
		} finally {
			try {
				stmt.close();
			} catch (Exception _ignore) {
			}
			try {
				result.close();
			} catch (Exception _ignore) {
			}
			try {
				conn.close();
			} catch (Exception _ignore) {
			}
		}
	}

	public static void executeUpdate(String query, Connection conn) throws SQLException {

		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(query);

		} catch (SQLException e) {
			throw e;
		} finally {
			try {
				stmt.close();
			} catch (Exception _ignore) {
			}
			try {
				conn.close();
			} catch (Exception _ignore) {
			}
		}
	}

	private static List<Map<String, Object>> resultSetToArrayList(ResultSet rs) throws SQLException {

		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		while (rs.next()) {
			Map<String, Object> row = new HashMap<String, Object>(columns);
			for (int i = 1; i <= columns; ++i) {
				row.put(md.getColumnName(i), rs.getObject(i));
			}
			list.add(row);
		}

		return list;
	}

	public static void execute(String query, Connection conn) throws SQLException {

		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(query);

		} catch (SQLException e) {
			throw e;
		} finally {
			try {
				stmt.close();
			} catch (Exception _ignore) {
			}
		}
	}
}
