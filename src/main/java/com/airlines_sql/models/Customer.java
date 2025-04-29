package com.airlines_sql.models;

import javafx.beans.property.*;
import java.time.LocalDate;

public class Customer {
	private final StringProperty firstName = new SimpleStringProperty();
	private final StringProperty lastName  = new SimpleStringProperty();
	private final ObjectProperty<LocalDate> birthDate = new SimpleObjectProperty<>();
	private final StringProperty email     = new SimpleStringProperty();
	private final StringProperty phone     = new SimpleStringProperty();

	public Customer(String firstName, String lastName, LocalDate birthDate, String email, String phone) {
		this.firstName.set(firstName);
		this.lastName.set(lastName);
		this.birthDate.set(birthDate);
		this.email.set(email);
		this.phone.set(phone);
	}

	// Геттеры для свойств (возвращают значения)
	public String getFirstName() { return firstName.get(); }
	public String getLastName()  { return lastName.get(); }
	public LocalDate getBirthDate() { return birthDate.get(); }
	public String getEmail()    { return email.get(); }
	public String getPhone()    { return phone.get(); }

	// Свойства (необязательно, но позволяют привязываться к ним напрямую)
	public StringProperty firstNameProperty() { return firstName; }
	public StringProperty lastNameProperty()  { return lastName;  }
	public ObjectProperty<LocalDate> birthDateProperty() { return birthDate; }
	public StringProperty emailProperty()     { return email;     }
	public StringProperty phoneProperty()     { return phone;     }
}
