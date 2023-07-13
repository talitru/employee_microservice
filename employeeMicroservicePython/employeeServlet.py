from flask import Flask, request, jsonify
import json
import mysql.connector
from mysql.connector import Error

app = Flask(__name__)

MYSQL_URL = "localhost"
userName = "tali"
passWord = "dtfd123"
databaseName = "employees_microservice"
CREATE_TABLE_FILE = "createEmployeeTable.sql"

def create_tables(connection):
    with open(CREATE_TABLE_FILE, 'r') as file:
        script = file.read()
        cursor = connection.cursor()
        cursor.execute(script)
        cursor.close()

def create_database():
    try:
        connection = mysql.connector.connect(host=MYSQL_URL, user=userName, password=passWord)
        cursor = connection.cursor()
        cursor.execute("CREATE DATABASE IF NOT EXISTS " + databaseName)
        cursor.close()
        connection.close()
        connection = mysql.connector.connect(host=MYSQL_URL, user=userName, password=passWord, database=databaseName)
        create_tables(connection)
        return connection
    except Error as e:
        raise RuntimeError(e)

databaseConnection = create_database()

@app.route('/employees', methods=['GET'])
def get_employees():
    cursor = databaseConnection.cursor(dictionary=True)
    cursor.execute("SELECT * FROM Employees")
    employees = cursor.fetchall()
    cursor.close()
    if len(employees) == 0:
        return jsonify({'error': 'There are no employees'})
    return jsonify({'employees': employees})

@app.route('/employees/<employee_id>', methods=['GET'])
def get_employee(employee_id):
    cursor = databaseConnection.cursor(dictionary=True)
    cursor.execute("SELECT * FROM Employees WHERE employee_id = %s", (employee_id,))
    employee = cursor.fetchone()
    cursor.close()
    if employee is None:
        return jsonify({'error': 'ID does not exist'})
    return jsonify(employee)

@app.route('/employees', methods=['POST'])
def add_employee():
    try:
        employee = request.get_json()
        if not is_json_valid(employee):
            return jsonify({'error': 'Invalid data format'})
        cursor = databaseConnection.cursor()
        cursor.execute("INSERT INTO Employees (name, country, city, salary) VALUES (%s, %s, %s, %s)",
                       (employee['name'], employee['country'], employee['city'], employee['salary']))
        databaseConnection.commit()
        cursor.close()
        return jsonify({'message': 'Employee successfully added'})
    except Error as e:
        return jsonify({'error': 'Internal server error'})


@app.route('/employees/<employee_id>', methods=['PUT'])
def update_employee(employee_id):
    try:
        employee = request.get_json()
        if not employee:
            return jsonify({'error': 'No data to update'})
        cursor = databaseConnection.cursor()
        
        # Check if employee_id exists in the table
        cursor.execute("SELECT employee_id FROM Employees WHERE employee_id = %s", (employee_id,))
        if cursor.fetchone() is None:
            return jsonify({'error': 'Employee not found'})
        
        updated = False  # Track whether any updates were made
        
        for key, value in employee.items():
            # Check if the key exists as a column in the table
            cursor.execute("SHOW COLUMNS FROM Employees LIKE %s", (key,))
            if cursor.fetchone() is None:
                return jsonify({'error': 'Invalid key: {key}'})
            
            cursor.execute("UPDATE Employees SET {} = %s WHERE employee_id = %s".format(key), (value, employee_id))
            if cursor.rowcount > 0:  # If at least one row was affected, set updated to True
                updated = True
        
        databaseConnection.commit()
        cursor.close()
        
        if not updated:  # If no updates were made, return an appropriate message
            return jsonify({'message': 'No changes made'})
        
        return jsonify({'message': 'Employee updated successfully'})
    except Error:
        return jsonify({'error': 'An error occurred while updating the employee'})


@app.route('/employees', methods=['DELETE'])
def delete_employees():
    cursor = databaseConnection.cursor()
    cursor.execute("DELETE FROM Employees")
    databaseConnection.commit()
    cursor.close()
    return jsonify({'message': 'All employees deleted successfully'})

@app.route('/employees/<employee_id>', methods=['DELETE'])
def delete_employee(employee_id):
    cursor = databaseConnection.cursor()
    cursor.execute("DELETE FROM Employees WHERE employee_id = %s", (employee_id,))
    if cursor.rowcount == 0:
        cursor.close()
        return jsonify({'message': 'Employee not found'})

    databaseConnection.commit()
    cursor.close()
    return jsonify({'message': 'Employee deleted successfully'})

def is_json_valid(employee):
    if 'name' not in employee or 'country' not in employee or 'city' not in employee or 'salary' not in employee:
        return False
    if not isinstance(employee['name'], str) or not isinstance(employee['country'], str) or not isinstance(employee['city'], str) or not isinstance(employee['salary'], (int, float)):
        return False
    return True

if __name__ == '__main__':
    app.run()
