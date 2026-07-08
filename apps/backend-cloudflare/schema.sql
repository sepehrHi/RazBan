PRAGMA defer_foreign_keys=TRUE;
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,         -- Unique username
    password_hash TEXT NOT NULL,         -- Password hash
    display_name TEXT,                   -- Display name
    avatar_url TEXT,                     -- Avatar URL in Backblaze B2
    status TEXT DEFAULT 'offline',        -- User status: online, offline, emergency
    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE chats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    chat_type TEXT NOT NULL,             -- Chat type: 'private' or 'group'
    title TEXT,                          -- Group title (for group chats)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE chat_participants (
    chat_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chat_id, user_id),
    FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    chat_id INTEGER NOT NULL,             -- Chat this message belongs to
    sender_id INTEGER NOT NULL,           -- Message sender
    network_mode TEXT NOT NULL,          -- Network mode: normal, timeout, emergency
    message_type TEXT NOT NULL,          -- Message type: text, voice, video, photo, file
    
    -- Message content:
    -- Text messages are stored directly here
    -- Media stores a Backblaze B2 download URL
    content TEXT NOT NULL,               
    
    -- Optional file metadata
    file_name TEXT,                      -- Original filename
    file_size INTEGER,                   -- File size in bytes
    metadata TEXT,                       -- Extra metadata as JSON
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP, is_edited INTEGER DEFAULT 0, reply_to_id INTEGER DEFAULT NULL,
    FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE message_receipts (
    message_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    is_read INTEGER DEFAULT 0,           -- 0 = unread, 1 = read
    read_at DATETIME,
    PRIMARY KEY (message_id, user_id),
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE user_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    token TEXT UNIQUE NOT NULL,          -- Client session token
    device_info TEXT,                     -- Device information (optional)
    expires_at DATETIME NOT NULL,         -- Token expiration
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE message_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    message_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,         -- User who performed the action
    action TEXT NOT NULL,              -- Action type: edit or delete_everyone
    old_content TEXT,                 -- Previous message content
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE message_deletions (
    user_id INTEGER NOT NULL,
    message_id INTEGER NOT NULL,
    PRIMARY KEY (user_id, message_id)
);
CREATE INDEX idx_messages_chat_id ON messages(chat_id);
CREATE INDEX idx_chat_participants_user_id ON chat_participants(user_id);
CREATE INDEX idx_sessions_token ON user_sessions(token);