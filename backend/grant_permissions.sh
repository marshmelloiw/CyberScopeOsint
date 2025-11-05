#!/bin/bash
# PostgreSQL İzin Verme Scripti

echo "🔐 PostgreSQL izinleri veriliyor..."

# PostgreSQL'e bağlan ve izinleri ver
psql -U postgres -d cyberscope -f grant_permissions.sql

if [ $? -eq 0 ]; then
    echo "✅ İzinler başarıyla verildi!"
else
    echo "❌ İzin verme işlemi başarısız oldu."
    echo "Manuel olarak çalıştırmak için:"
    echo "psql -U postgres -d cyberscope -f grant_permissions.sql"
fi

