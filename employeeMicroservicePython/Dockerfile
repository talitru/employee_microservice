# Use an official Python runtime as the base image
FROM python

# Set the working directory in the container
WORKDIR /app

# Install the Python dependencies
RUN pip install mysql-connector-python
RUN pip install Flask

# Copy the application code to the container
COPY . .

# Expose the port on which the Flask app will run (default is 5000)
EXPOSE 5000

# Start the Flask app
CMD ["python", "./employeeServlet.py"]
