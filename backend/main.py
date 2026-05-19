import sqlite3
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List

app = FastAPI()

# Database setup
DB_NAME = "movies.db"

def init_db():
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    # Table for watched movies
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS watched (
            user_id TEXT,
            movie_id INTEGER,
            title TEXT,
            date TEXT,
            poster TEXT,
            rating REAL,
            isFavourite INTEGER,
            PRIMARY KEY (user_id, movie_id)
        )
    ''')
    # Table for user profiles
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS profiles (
            user_id TEXT PRIMARY KEY,
            name TEXT,
            email TEXT
        )
    ''')
    conn.commit()
    conn.close()

init_db()

class Movie(BaseModel):
    id: int
    title: str
    date: str
    poster: str
    rating: float
    isFavourite: bool = False

class UserProfile(BaseModel):
    name: str
    email: str

@app.get("/watched/{user_id}", response_model=List[Movie])
def get_watched(user_id: str):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute("SELECT movie_id, title, date, poster, rating, isFavourite FROM watched WHERE user_id = ?", (user_id,))
    rows = cursor.fetchall()
    conn.close()

    return [
        Movie(id=row[0], title=row[1], date=row[2], poster=row[3], rating=row[4], isFavourite=bool(row[5]))
        for row in rows
    ]

@app.post("/watched/{user_id}")
def add_watched(user_id: str, movie: Movie):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    try:
        cursor.execute('''
            INSERT INTO watched (user_id, movie_id, title, date, poster, rating, isFavourite)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (user_id, movie.id, movie.title, movie.date, movie.poster, movie.rating, int(movie.isFavourite)))
        conn.commit()
    except sqlite3.IntegrityError:
        return {"status": "already exists"}
    finally:
        conn.close()
    return {"status": "success"}

@app.patch("/watched/{user_id}/{movie_id}")
def update_favorite(user_id: str, movie_id: int, is_fav: bool):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute("UPDATE watched SET isFavourite = ? WHERE user_id = ? AND movie_id = ?", (int(is_fav), user_id, movie_id))
    if cursor.rowcount == 0:
        conn.close()
        raise HTTPException(status_code=404, detail="Movie not found")
    conn.commit()
    conn.close()
    return {"status": "updated"}

@app.get("/profile/{user_id}", response_model=UserProfile)
def get_profile(user_id: str):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute("SELECT name, email FROM profiles WHERE user_id = ?", (user_id,))
    row = cursor.fetchone()
    conn.close()

    if not row:
        raise HTTPException(status_code=404, detail="Profile not found")
    return UserProfile(name=row[0], email=row[1])

@app.post("/profile/{user_id}")
def save_profile(user_id: str, profile: UserProfile):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute('''
        INSERT OR REPLACE INTO profiles (user_id, name, email)
        VALUES (?, ?, ?)
    ''', (user_id, profile.name, profile.email))
    conn.commit()
    conn.close()
    return {"status": "success"}
