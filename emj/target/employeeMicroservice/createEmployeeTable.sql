
CREATE TABLE IF NOT EXISTS Employees(
    employee_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    salary DOUBLE NOT NULL,
    PRIMARY KEY (employee_id)
);