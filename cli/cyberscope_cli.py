import os
import sys
import argparse
import requests

API_BASE = os.getenv('CYBERSCOPE_API', 'http://localhost:8080')
API_KEY = os.getenv('CYBERSCOPE_API_KEY', '')

def req(method, path, json=None):
    headers = {'Content-Type': 'application/json'}
    if API_KEY:
        headers['X-API-KEY'] = API_KEY
    url = API_BASE + path
    r = requests.request(method, url, json=json, headers=headers, timeout=10)
    r.raise_for_status()
    return r.json() if r.content else {}


def cmd_login(args):
    data = req('POST', '/api/auth/login', json={'email': args.email, 'password': args.password})
    print(data)


def cmd_score(args):
    payload = {
        'provider': {
            'virustotal_detected': args.vt_detected,
            'virustotal_total': args.vt_total,
            'shodan_open_ports': args.open_ports,
            'shodan_high_risk_ports': args.high_risk_ports,
            'hibp_breach_count': args.hibp
        },
        'context': {'asset_type': args.asset}
    }
    data = req('POST', '/api/threat/score', json=payload)
    print(data)


def main():
    p = argparse.ArgumentParser(description='CyberScope OSINT CLI')
    sub = p.add_subparsers(dest='cmd', required=True)

    plogin = sub.add_parser('login', help='Login user')
    plogin.add_argument('--email', required=True)
    plogin.add_argument('--password', required=True)
    plogin.set_defaults(func=cmd_login)

    pscore = sub.add_parser('score', help='Compute threat score')
    pscore.add_argument('--asset', default='domain')
    pscore.add_argument('--vt-detected', type=int, default=0)
    pscore.add_argument('--vt-total', type=int, default=0)
    pscore.add_argument('--open-ports', type=int, default=0)
    pscore.add_argument('--high-risk-ports', type=int, default=0)
    pscore.add_argument('--hibp', type=int, default=0)
    pscore.set_defaults(func=cmd_score)

    args = p.parse_args()
    try:
        args.func(args)
    except requests.HTTPError as e:
        print(f'HTTP error: {e.response.status_code} {e.response.text}', file=sys.stderr)
        sys.exit(1)

if __name__ == '__main__':
    main()
