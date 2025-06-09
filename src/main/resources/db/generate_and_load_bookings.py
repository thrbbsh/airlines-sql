import psycopg2
import pandas as pd
import random
from datetime import datetime, timedelta
import os # Import the module for file path operations

DB_CONFIG = {
    "dbname": "airlinedb",
    "user": "volochai",
    "password": "254849",
    "host": "localhost",
    "port": "5432",
    "options": "-c statement_timeout=30000"
}

# --- GENERATION SETTINGS ---
MIN_BOOKING_PERCENTAGE = 0.30
MAX_BOOKING_PERCENTAGE = 0.85
CSV_FILENAME = 'booking.csv'

def get_db_connection():
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        print("✅ Successfully connected to the PostgreSQL database.")
        return conn
    except psycopg2.OperationalError as e:
        print(f"❌ Connection error: {e}")
        return None

def fetch_data(conn):
    print("... 1/4: Fetching data from the ticket and customer tables...")
    tickets_sql = """
        SELECT t.ticket_id, t.instance_id
        FROM ticket t
        LEFT JOIN booking b ON t.ticket_id = b.ticket_id
        WHERE b.ticket_id IS NULL AND (SELECT fi.scheduled_departure FROM flight_instance fi WHERE fi.instance_id = t.instance_id) > NOW()
    """
    customers_sql = "SELECT customer_id FROM customer"
    try:
        tickets_df = pd.read_sql(tickets_sql, conn)
        customers_df = pd.read_sql(customers_sql, conn)
        print(f"✅ Found {len(tickets_df)} available tickets and {len(customers_df)} customers.")
        return tickets_df, customers_df['customer_id'].tolist()
    except Exception as e:
        print(f"❌ Error while fetching data: {e}")
        return pd.DataFrame(), []

def generate_and_load_from_file():
    all_bookings = []

    with get_db_connection() as conn:
        if not conn:
            return

        tickets_df, customer_ids = fetch_data(conn)

        if tickets_df.empty or not customer_ids:
            print("Not enough data to create bookings. Exiting.")
            return

        print("... 2/4: Generating bookings in memory...")
        for instance_id, flight_tickets_df in tickets_df.groupby('instance_id'):
            booking_percentage = random.uniform(MIN_BOOKING_PERCENTAGE, MAX_BOOKING_PERCENTAGE)
            num_tickets_to_book = int(len(flight_tickets_df) * booking_percentage)
            tickets_to_book = flight_tickets_df.sample(n=num_tickets_to_book)

            for index, ticket_row in tickets_to_book.iterrows():
                customer_id = random.choice(customer_ids)
                booking_date = datetime.now() - timedelta(days=random.randint(0, 10), hours=random.randint(0, 23))
                status = random.choices(['Confirmed', 'Pending', 'Cancelled'], weights=[85, 10, 5], k=1)[0]

                all_bookings.append({
                    'customer_id': customer_id,
                    'booking_date': booking_date.strftime('%Y-%m-%d %H:%M:%S'),
                    'status': status,
                    'ticket_id': ticket_row['ticket_id']
                })

        if not all_bookings:
            print("No bookings were generated.")
            return

        bookings_df = pd.DataFrame(all_bookings)
        print(f"✅ Generated {len(bookings_df)} bookings.")

        print(f"... 3/4: Saving data to the local file '{CSV_FILENAME}'...")

        columns_to_save = ['customer_id', 'booking_date', 'status', 'ticket_id']
        bookings_df[columns_to_save].to_csv(CSV_FILENAME, index=False, header=True)

        print(f"✅ File '{CSV_FILENAME}' created successfully.")

        print(f"... 4/4: Loading the file '{CSV_FILENAME}' into the 'booking' table...")

        try:
            with conn.cursor() as cursor:
                with open(CSV_FILENAME, 'r') as f:
                    sql_copy = f"COPY booking({', '.join(columns_to_save)}) FROM STDIN WITH (FORMAT CSV, HEADER)"
                    cursor.copy_expert(sql=sql_copy, file=f)

            conn.commit()
            print("✅ Data from the file has been successfully loaded into the database!")

        except (Exception, psycopg2.DatabaseError) as error:
            print(f"❌ Error while loading the file into the DB: {error}")
            conn.rollback()
            return

    print("\n🎉 Done! The process of generating, saving, and loading is complete.")

if __name__ == '__main__':
    generate_and_load_from_file()