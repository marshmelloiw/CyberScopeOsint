"""
Mermaid diyagramlarını görselleştirip Word dosyasına ekleyen script.
"""
import argparse
import base64
import io
import re
import urllib3
from pathlib import Path
from typing import List, Optional

import requests
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from PIL import Image
SVGLIB_AVAILABLE = False
try:
    from svglib.svglib import svg2rlg
    from reportlab.graphics import renderPM
    SVGLIB_AVAILABLE = True
except (ImportError, OSError, Exception) as e:
    SVGLIB_AVAILABLE = False
    print(f"[BILGI] SVG kutuphanesi yuklenemedi: {e}")

# SSL uyarılarını bastır
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


def mermaid_to_png(mermaid_code: str) -> Optional[bytes]:
    """
    Mermaid kodunu doğrudan PNG'ye dönüştürür (mermaid.ink API kullanarak).
    """
    # Mermaid kodunu base64 encode et
    graph_bytes = mermaid_code.encode('utf8')
    base64_bytes = base64.urlsafe_b64encode(graph_bytes)
    base64_string = base64_bytes.decode("ascii")
    
    # mermaid.ink API'sine PNG isteği gönder
    url = f"https://mermaid.ink/img/{base64_string}"
    
    try:
        # SSL doğrulamasını devre dışı bırak (sadece bu script için)
        response = requests.get(url, timeout=60, verify=False)
        response.raise_for_status()
        
        # İçeriğin PNG olup olmadığını kontrol et
        content_type = response.headers.get('Content-Type', '')
        if 'image/png' in content_type or response.content[:8] == b'\x89PNG\r\n\x1a\n':
            return response.content
        else:
            # HTML hata sayfası olabilir
            if b'<html' in response.content[:100].lower() or b'<!doctype' in response.content[:100].lower():
                return None
            # Diğer durumlarda içeriği döndür
            return response.content
    except requests.exceptions.HTTPError as e:
        # 414 URI Too Long hatası alırsak alternatif yöntem dene
        if e.response.status_code == 414:
            return None
        elif e.response.status_code == 503:
            # Service Unavailable - geçici hata, None döndür
            return None
        else:
            return None
    except Exception as e:
        return None


def mermaid_to_svg(mermaid_code: str) -> Optional[bytes]:
    """
    Mermaid kodunu SVG'ye dönüştürür (mermaid.ink API kullanarak).
    """
    # Mermaid kodunu base64 encode et
    graph_bytes = mermaid_code.encode('utf8')
    base64_bytes = base64.urlsafe_b64encode(graph_bytes)
    base64_string = base64_bytes.decode("ascii")
    
    # mermaid.ink API'sine istek gönder
    url = f"https://mermaid.ink/svg/{base64_string}"
    
    try:
        # SSL doğrulamasını devre dışı bırak (sadece bu script için)
        response = requests.get(url, timeout=30, verify=False)
        response.raise_for_status()
        return response.content
    except requests.exceptions.HTTPError as e:
        # 414 URI Too Long hatası alırsak, alternatif yöntem dene
        if e.response.status_code == 414:
            print(f"  [UYARI] URI cok uzun, alternatif yontem deneniyor...")
            # Mermaid kodunu sıkıştırarak tekrar dene (gereksiz boşlukları kaldır)
            compressed_code = re.sub(r'\n\s*\n+', '\n', mermaid_code)  # Çift boş satırları tek yap
            compressed_code = re.sub(r' +', ' ', compressed_code)  # Birden fazla boşluğu tek yap
            compressed_code = re.sub(r'\n\s+', '\n', compressed_code)  # Satır başındaki boşlukları kaldır
            compressed_code = compressed_code.strip()
            
            if len(compressed_code) < len(mermaid_code):
                graph_bytes = compressed_code.encode('utf8')
                base64_bytes = base64.urlsafe_b64encode(graph_bytes)
                base64_string = base64_bytes.decode("ascii")
                if len(base64_string) < 8000:  # Hala çok uzunsa deneme
                    url = f"https://mermaid.ink/svg/{base64_string}"
                    try:
                        response = requests.get(url, timeout=60, verify=False)
                        response.raise_for_status()
                        return response.content
                    except:
                        pass
            
            # Hala çok uzunsa, sadece class tanımlarını ve ilişkileri tut (field'ları kaldır)
            print(f"  [UYARI] Agresif sikistirma deneniyor...")
            lines = compressed_code.split('\n')
            minimal_lines = []
            for line in lines:
                stripped = line.strip()
                if not stripped:
                    continue
                if stripped.startswith('classDiagram') or stripped.startswith('direction'):
                    minimal_lines.append(stripped)
                elif stripped.startswith('class '):
                    # Sadece class tanımını al, field'ları kaldır
                    if '{' in stripped:
                        class_def = stripped.split('{')[0].strip() + ' {}'
                        minimal_lines.append(class_def)
                    else:
                        minimal_lines.append(stripped)
                elif '-->' in stripped or '..>' in stripped:
                    minimal_lines.append(stripped)
            
            minimal_code = '\n'.join(minimal_lines)
            graph_bytes = minimal_code.encode('utf8')
            base64_bytes = base64.urlsafe_b64encode(graph_bytes)
            base64_string = base64_bytes.decode("ascii")
            if len(base64_string) < 8000:
                url = f"https://mermaid.ink/svg/{base64_string}"
                try:
                    response = requests.get(url, timeout=60, verify=False)
                    response.raise_for_status()
                    print(f"  [OK] Minimal versiyon SVG olarak alindi.")
                    return response.content
                except:
                    pass
            
            print(f"  [HATA] URI cok uzun, SVG alinamadi. Dosya cok buyuk.")
            return None
        else:
            print(f"  [HATA] Mermaid SVG donusturme hatasi: {e}")
            return None
    except Exception as e:
        print(f"  [HATA] Mermaid SVG donusturme hatasi: {e}")
        return None


def svg_to_png(svg_content: bytes, width: int = 1200) -> Optional[bytes]:
    """
    SVG içeriğini PNG'ye dönüştürür.
    """
    if not SVGLIB_AVAILABLE:
        print("  [UYARI] SVG kutuphanesi yuklu degil, SVG'den PNG'ye donusturulemiyor.")
        return None
        
    try:
        # ReportLab kullan
        drawing = svg2rlg(io.BytesIO(svg_content))
        
        if drawing is None:
            print("  [UYARI] SVG donusturme basarisiz: drawing None")
            return None
        
        # PNG'ye render et - daha küçük DPI ile dene (büyük diyagramlar için)
        img_data = io.BytesIO()
        try:
            renderPM.drawToFile(drawing, img_data, fmt='PNG', dpi=100)
        except Exception as dpi_error:
            # DPI hatası varsa daha düşük DPI ile dene
            print(f"  [UYARI] DPI hatasi, dusuk cozunurlukle deneniyor: {dpi_error}")
            img_data = io.BytesIO()
            renderPM.drawToFile(drawing, img_data, fmt='PNG', dpi=72)
        
        img_data.seek(0)
        
        # PNG'yi yeniden boyutlandır (gerekirse)
        img = Image.open(img_data)
        # Genişliği ayarla, yüksekliği orantılı olarak ayarla
        if img.width > width:
            ratio = width / img.width
            new_height = int(img.height * ratio)
            img = img.resize((width, new_height), Image.Resampling.LANCZOS)
        
        # BytesIO'ya kaydet
        output = io.BytesIO()
        img.save(output, format='PNG')
        output.seek(0)
        
        return output.read()
    except Exception as e:
        print(f"  [HATA] SVG to PNG donusturme hatasi: {e}")
        return None


def clean_mermaid_code(content: str) -> str:
    """
    Mermaid kodunu temizler ve düzenler.
    """
    # Markdown wrapper'ları kaldır (::: mermaid gibi)
    content = re.sub(r'^:::?\s*mermaid\s*$', '', content, flags=re.MULTILINE)
    content = re.sub(r'^:::?\s*$', '', content, flags=re.MULTILINE)
    
    # Başta ve sonda boş satırları temizle
    content = content.strip()
    
    # Boş sınıf tanımlarını düzelt (sadece {} olanlar)
    lines = content.split('\n')
    cleaned_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        # Boş sınıf tanımı kontrolü
        if line.strip().startswith('class ') and i + 1 < len(lines):
            if lines[i + 1].strip() == '}':
                # Boş sınıf, düzelt
                cleaned_lines.append(line)
                cleaned_lines.append('}')
                i += 2
                continue
        cleaned_lines.append(line)
        i += 1
    
    return '\n'.join(cleaned_lines)


def split_large_diagram(mermaid_code: str, max_lines: int = 80) -> List[str]:
    """
    Büyük diyagramları parçalara böler.
    URI uzunluğu sınırı nedeniyle gerekli.
    """
    lines = mermaid_code.split('\n')
    
    # Önce URI uzunluğunu kontrol et
    test_bytes = mermaid_code.encode('utf8')
    test_base64 = base64.urlsafe_b64encode(test_bytes)
    test_string = test_base64.decode("ascii")
    # URL uzunluğu yaklaşık 7000 karakterden fazlaysa böl (mermaid.ink limiti ~8192, güvenli marj)
    if len(test_string) < 7000:
        return [mermaid_code]
    
    # Diyagramı mantıklı yerlerden böl - daha basit ve güvenli yöntem
    parts = []
    header_lines = []
    class_blocks = []  # Her sınıf bloğu (class tanımı + içeriği)
    relationships = []
    
    # Header satırlarını bul
    for line in lines[:10]:
        stripped = line.strip()
        if stripped.startswith('classDiagram') or stripped.startswith('direction'):
            header_lines.append(line)
    
    # Sınıf bloklarını ve ilişkileri ayır
    current_class_block = []
    in_class = False
    
    for line in lines:
        stripped = line.strip()
        if stripped.startswith('class '):
            # Önceki sınıf bloğunu kaydet
            if current_class_block:
                class_blocks.append('\n'.join(current_class_block))
            current_class_block = [line]
            in_class = True
        elif in_class:
            current_class_block.append(line)
            if stripped == '}':
                in_class = False
        elif stripped and ('-->' in stripped or '..>' in stripped):
            relationships.append(line)
    
    # Son sınıf bloğunu ekle
    if current_class_block:
        class_blocks.append('\n'.join(current_class_block))
    
    # Eğer sınıf sayısı çok fazla değilse, bölme
    if len(class_blocks) <= 15:
        return [mermaid_code]
    
    # Her parçaya yaklaşık 10-12 sınıf ekle (URI uzunluğu için daha küçük)
    classes_per_part = max(10, len(class_blocks) // 4)
    
    for i in range(0, len(class_blocks), classes_per_part):
        part_lines = header_lines.copy()
        part_lines.append('')  # Boş satır
        part_classes = class_blocks[i:i + classes_per_part]
        part_lines.extend(part_classes)
        
        # Bu parçadaki sınıf alias'larını bul
        part_aliases = set()
        for class_block in part_classes:
            for line in class_block.split('\n'):
                if 'as ' in line:
                    alias = line.split('as ')[1].split()[0].strip()
                    part_aliases.add(alias)
        
        # İlgili ilişkileri ekle
        part_lines.append('')  # Boş satır
        for rel in relationships:
            # İlişkide geçen alias'ları kontrol et
            rel_aliases = [alias for alias in part_aliases if alias in rel]
            if len(rel_aliases) >= 1:  # En az bir alias bu parçada olmalı
                part_lines.append(rel)
        
        parts.append('\n'.join(part_lines))
    
    return parts if len(parts) > 1 else [mermaid_code]


def save_diagram_as_svg(output_dir: Path, title: str, mermaid_code: str):
    """
    Diyagramı SVG olarak kaydeder.
    """
    # Diyagramı bölmeden kaydet
    _save_single_diagram_as_svg(output_dir, mermaid_code, title)


def _save_single_diagram_as_svg(output_dir: Path, mermaid_code: str, title: str = ""):
    """
    Tek bir diyagramı SVG olarak kaydeder.
    """
    display_title = title if title else "Diyagram"
    
    # Mermaid kodunu temizle
    cleaned_code = clean_mermaid_code(mermaid_code)
    
    # SVG al
    print(f"  {display_title} SVG olarak kaydediliyor...")
    svg_content = mermaid_to_svg(cleaned_code)
    
    if svg_content is None:
        print(f"  [UYARI] {display_title} diyagrami olusturulamadi.")
        return
    
    # SVG'yi kaydet
    try:
        output_dir.mkdir(parents=True, exist_ok=True)
        safe_title = "".join(c for c in display_title if c.isalnum() or c in (' ', '-', '_')).strip()
        safe_title = safe_title.replace(' ', '_').replace('(', '').replace(')', '').replace('ı', 'i').replace('ş', 's').replace('ğ', 'g').replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
        svg_file = output_dir / f"{safe_title}.svg"
        svg_file.write_bytes(svg_content)
        print(f"  [OK] {display_title} SVG olarak kaydedildi: {svg_file}")
    except Exception as e:
        print(f"  [HATA] SVG kaydedilirken hata: {e}")


def get_diagram_title(filename: str) -> str:
    """
    Dosya adından diyagram başlığını oluşturur.
    """
    # Dosya uzantısını kaldır
    name = Path(filename).stem
    
    # Türkçe başlıklar
    title_map = {
        'controllers': 'Controller Diyagramı',
        'controllers_noempty': 'Controller Diyagramı (Sadeleştirilmiş)',
        'models': 'Model Diyagramı',
        'repositories': 'Repository Diyagramı',
        'services': 'Service Diyagramı',
    }
    
    return title_map.get(name, name.replace('_', ' ').title() + ' Diyagramı')


def main():
    parser = argparse.ArgumentParser(
        description="Mermaid diyagramlarını görselleştirip Word dosyasına ekler"
    )
    parser.add_argument(
        "--input-dir",
        type=Path,
        default=Path("docs/uml"),
        help="Mermaid dosyalarının bulunduğu dizin",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("docs/uml/svg_output"),
        help="SVG dosyalarının kaydedileceği dizin",
    )
    parser.add_argument(
        "--include",
        nargs="*",
        help="Dahil edilecek dosya adları (varsayılan: tüm .mmd dosyaları)",
    )
    
    args = parser.parse_args()
    
    input_dir = args.input_dir.resolve()
    if not input_dir.exists():
        raise SystemExit(f"Giriş dizini bulunamadı: {input_dir}")
    
    # MMD dosyalarını bul
    mmd_files: List[Path] = []
    if args.include:
        for name in args.include:
            file_path = input_dir / name
            if file_path.exists():
                mmd_files.append(file_path)
            else:
                print(f"[UYARI] Dosya bulunamadi: {file_path}")
    else:
        mmd_files = sorted(input_dir.glob("*.mmd"))
    
    if not mmd_files:
        raise SystemExit(f"MMD dosyası bulunamadı: {input_dir}")
    
    print(f"{len(mmd_files)} MMD dosyasi bulundu.")
    
    # Çıktı dizinini oluştur
    output_dir = args.output_dir.resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    print(f"SVG dosyalari kaydedilecek: {output_dir}\n")
    
    # Her MMD dosyasını işle
    for mmd_file in mmd_files:
        print(f"İşleniyor: {mmd_file.name}")
        
        try:
            content = mmd_file.read_text(encoding='utf-8')
            cleaned_content = clean_mermaid_code(content)
            
            if not cleaned_content.strip():
                print(f"  [UYARI] Dosya bos, atlaniyor.")
                continue
            
            title = get_diagram_title(mmd_file.name)
            save_diagram_as_svg(output_dir, title, cleaned_content)
                
        except Exception as e:
            print(f"  [HATA] Hata: {e}")
    
    print(f"\n[OK] Tum SVG dosyalari kaydedildi: {output_dir}")


if __name__ == "__main__":
    main()

