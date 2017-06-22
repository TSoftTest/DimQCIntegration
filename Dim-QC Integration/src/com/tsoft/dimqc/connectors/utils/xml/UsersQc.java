package com.tsoft.dimqc.connectors.utils.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "users" })
@XmlRootElement(name = "Users")
public class UsersQc {

	@XmlElement(name = "User", required = true)
	private List<UsersQc.User> users = new ArrayList<UsersQc.User>();

	public UsersQc() {
	}

	public void addListsQc(UsersQc.User user) {
		users.add(user);
	}

	public List<UsersQc.User> getUser() {
		return users;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class User {

		@XmlAttribute(name = "Name", required = true)
		private String name;

		@XmlAttribute(name = "FullName", required = true)
		private String fullName;

		@XmlElement(name = "email", required = true)
		private String email;

		@XmlElement(name = "phone", required = true)
		private String phone;

		public User() {
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getFullName() {
			return fullName;
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}
	}
}
