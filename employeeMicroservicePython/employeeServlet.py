from flask import Flask, request, jsonify
import json
import mysql.connector
from mysql.connector import Error

app = Flask(__name__)

MYSQL_URL = "localhost"
username = "tali"
password = "dtfd123"
databaseName = "employees_microservice"
CREATE_TABLE_PATH = "/home/tali/Desktop/employeeMicroservice/employeeMicroservice/src/main/webapp/createEmployeeTable.sql"

def create_tables(connection):
    with open(CREATE_TABLE_PATH, 'r') as file:
        script = file.read()
        cursor = connection.cursor()
        cursor.execute(script)
        cursor.close()

def create_database():
    try:
        connection = mysql.connector.connect(host=MYSQL_URL, user=username, password=password)
        cursor = connection.cursor()
        cursor.execute("CREATE DATABASE IF NOT EXISTS " + databaseName)
        cursor.close()
        connection.close()
        connection = mysql.connector.connect(host=MYSQL_URL, user=username, password=password, database=databaseName)
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
        if 'employee_id' in employee:
            del employee['employee_id']
        if not employee:
            return jsonify({'error': 'No data to update'})
        cursor = databaseConnection.cursor()
        for key, value in employee.items():
            cursor.execute("UPDATE Employees SET {} = %s WHERE employee_id = %s".format(key), (value, employee_id))
        databaseConnection.commit()
        cursor.close()
        return jsonify({'message': 'Employee updated successfully'})
    except Error:
        return jsonify({'error': 'One of the keys are wrong'})

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