# FarmConnect Backend

Contract Farming System API Server

## Setup

```bash
# Install dependencies
npm install

# Initialize database
npm run db:init

# Start development server
npm run dev

# Build for production
npm run build

# Start production server
npm start
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login
- `GET /api/auth/me` - Get current user
- `PUT /api/auth/profile` - Update profile
- `POST /api/auth/change-password` - Change password

### Listings
- `GET /api/listings` - Browse all active listings
- `GET /api/listings/my` - Get farmer's own listings
- `GET /api/listings/:id` - Get listing details
- `POST /api/listings` - Create listing (Farmer)
- `PUT /api/listings/:id` - Update listing
- `PATCH /api/listings/:id/status` - Change listing status

### Negotiations
- `GET /api/negotiations` - Get user's negotiations
- `GET /api/negotiations/:id` - Get negotiation with messages
- `POST /api/negotiations` - Start negotiation (Buyer)
- `POST /api/negotiations/:id/messages` - Send message
- `POST /api/negotiations/:id/accept` - Accept terms
- `POST /api/negotiations/:id/reject` - Reject negotiation

### Contracts
- `GET /api/contracts` - Get user's contracts
- `GET /api/contracts/:id` - Get contract details
- `POST /api/contracts/:id/confirm` - Confirm contract
- `PATCH /api/contracts/:id/status` - Update contract status

### Fulfillment
- `GET /api/contracts/:id/fulfillment` - Get milestones
- `POST /api/contracts/:id/fulfillment/milestones` - Add milestone
- `PUT /api/contracts/fulfillment/milestones/:id` - Update milestone

### Payments
- `GET /api/contracts/:id/payments` - Get payments
- `POST /api/contracts/:id/payments` - Record payment

### Disputes
- `GET /api/disputes` - Get user's disputes
- `GET /api/disputes/:id` - Get dispute details
- `POST /api/disputes` - Raise dispute
- `POST /api/disputes/:id/evidence` - Add evidence

### Admin
- `GET /api/admin/stats` - Dashboard statistics
- `GET /api/admin/users` - List users
- `PUT /api/admin/users/:id/status` - Update user status
- `GET /api/admin/listings` - List all listings
- `PUT /api/admin/listings/:id/moderate` - Moderate listing
- `GET /api/admin/disputes` - List all disputes
- `PUT /api/admin/disputes/:id/resolve` - Resolve dispute
- `GET /api/admin/audit-logs` - View audit logs

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| PORT | Server port | 3000 |
| JWT_SECRET | JWT signing secret | (required) |
| JWT_EXPIRES_IN | Token expiry | 24h |
| DB_PATH | SQLite database path | ./data/farmconnect.db |
| UPLOAD_PATH | File upload directory | ./uploads |
| MAX_FILE_SIZE | Max upload size (bytes) | 10485760 |
