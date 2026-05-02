const Database = require('better-sqlite3');
const path = require('path');

const dbPath = path.join(__dirname, 'data', 'farmconnect.db');
const db = new Database(dbPath);

try {
    const user = db.prepare('SELECT email, password, role, name FROM users WHERE email = ?').get('buyer@gmail.com');

    if (user) {
        console.log('✅ Account found!');
        console.log('====================');
        console.log('Email:', user.email);
        console.log('Name:', user.name);
        console.log('Role:', user.role);
        console.log('Password Hash:', user.password);
        console.log('====================');
        console.log('\nNote: The password is hashed. For test accounts, the default password is usually "password123"');
    } else {
        console.log('❌ No account found for buyer@gmail.com');
    }
} catch (error) {
    console.error('Error:', error.message);
} finally {
    db.close();
}
