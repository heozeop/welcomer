// MongoDB initialization script for Welcomer SNS Feed System
// This script creates application users and sets up initial configuration

// Switch to the application database
db = db.getSiblingDB('welcomer_db');

// Create application user with read/write permissions
db.createUser({
  user: 'welcomer_app',
  pwd: 'apppassword',
  roles: [
    { role: 'readWrite', db: 'welcomer_db' },
    { role: 'dbAdmin', db: 'welcomer_db' }
  ]
});

// Create read-only user for analytics
db.createUser({
  user: 'welcomer_readonly',
  pwd: 'readonlypassword',
  roles: [{ role: 'read', db: 'welcomer_db' }]
});

// Create initial collections with validation rules
db.createCollection('content_cache', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['contentId', 'cachedAt', 'expiresAt'],
      properties: {
        contentId: { bsonType: 'string' },
        cachedAt: { bsonType: 'date' },
        expiresAt: { bsonType: 'date' },
        data: { bsonType: 'object' }
      }
    }
  }
});

db.createCollection('user_sessions', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['userId', 'sessionId', 'createdAt'],
      properties: {
        userId: { bsonType: 'string' },
        sessionId: { bsonType: 'string' },
        createdAt: { bsonType: 'date' },
        expiresAt: { bsonType: 'date' },
        deviceInfo: { bsonType: 'object' }
      }
    }
  }
});

db.createCollection('analytics_events', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['eventType', 'timestamp', 'data'],
      properties: {
        eventType: { bsonType: 'string' },
        timestamp: { bsonType: 'date' },
        userId: { bsonType: 'string' },
        data: { bsonType: 'object' }
      }
    }
  }
});

// Create indexes for better performance
db.content_cache.createIndex({ 'contentId': 1 }, { unique: true });
db.content_cache.createIndex({ 'expiresAt': 1 }, { expireAfterSeconds: 0 });

db.user_sessions.createIndex({ 'userId': 1 });
db.user_sessions.createIndex({ 'sessionId': 1 }, { unique: true });
db.user_sessions.createIndex({ 'expiresAt': 1 }, { expireAfterSeconds: 0 });

db.analytics_events.createIndex({ 'eventType': 1, 'timestamp': -1 });
db.analytics_events.createIndex({ 'userId': 1, 'timestamp': -1 });

print('MongoDB initialization completed successfully');