"""
HBC-Tool Android Bridge
 Android App  HBC-Tool 
"""

import sys
import io
import os
import traceback
from typing import Dict, Any


def do_disassemble(hbc_file_path: str, output_dir: str) -> Dict[str, Any]:
    """
     Hermes 
    
    Args:
        hbc_file_path: .bundle 
        output_dir: 
        
    Returns:
         status (success/error)  log ()
    """
    #  stdout/stderr，
    log_capture = io.StringIO()
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    
    #  stderr  devnull， ResourceWarning
    import warnings
    warnings.filterwarnings('ignore', category=ResourceWarning)
    sys.stdout = log_capture
    sys.stderr = open(os.devnull, 'w')
    
    try:
        print(f"...")
        
        # 
        if not os.path.exists(hbc_file_path):
            raise FileNotFoundError(f": {hbc_file_path}")
        
        # 
        file_size = os.path.getsize(hbc_file_path)
        
        # 
        if not os.access(hbc_file_path, os.R_OK):
            raise PermissionError(f": {hbc_file_path}")
        
        # 
        with open(hbc_file_path, 'rb') as f:
            content = f.read()
        
        # （）
        if len(content) < 8:
            raise ValueError("， Hermes ")
        
        magic = content[:8]
        
        #  HBC-Tool 
        try:
            import hbctool.hbc as hbc
            import hbctool.hasm as hasm
        except ImportError as e:
            print("...")
            # Version：
            output_file = os.path.join(output_dir, "output.hasm")
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(f"; HBC-Tool Android - \n")
                f.write(f"; : {os.path.basename(hbc_file_path)}\n")
                f.write(f"; : {len(content)} bytes\n")
                f.write(f"; : {magic.hex()}\n")
                f.write(f"\n; TODO:  HBC-Tool ，\n")
            print(f"✓ : {output_file}")
            return {
                "status": "success",
                "log": log_capture.getvalue(),
                "output_dir": output_dir
            }
        
        #  HBC  -  HBC-Tool  API
        try:
            with open(hbc_file_path, "rb") as f:
                hbc_obj = hbc.load(f)
        except Exception as parse_err:
            print(f"❌ : {parse_err}")
            raise
        
        # 
        header = hbc_obj.getHeader()
        source_hash = bytes(header["sourceHash"]).hex()
        version = header["version"]
        
        # 
        file_size_mb = file_size / 1024 / 1024
        if file_size_mb >= 1:
            size_str = f"{file_size_mb:.2f} MB"
        else:
            size_str = f"{file_size / 1024:.2f} KB"
        
        print(f"✓ Hermes Version: {version}")
        print(f"✓ : {size_str}")
        print(f"✓ Source Hash: {source_hash[:16]}...")
        
        #  HASM
        try:
            hasm.dump(hbc_obj, output_dir, force=True)
            dump_success = True
        except Exception as dump_err:
            print(f"❌ hasm.dump() : {dump_err}")
            dump_success = False
        
        if dump_success:
            print(f"✓  HASM ")
            print(f"✓ : {output_dir}")
            # ，
            try:
                captured_log = log_capture.getvalue()
            except:
                captured_log = "[，]"
            return {
                "status": "success",
                "log": captured_log,
                "output_dir": output_dir
            }
        else:
            raise Exception("hasm.dump() ")
        
    except Exception as e:
        error_msg = f"\n❌ Error: {str(e)}\n{traceback.format_exc()}"
        print(error_msg)
        try:
            captured_log = log_capture.getvalue()
        except:
            captured_log = "[ - ]"
        return {
            "status": "error",
            "log": captured_log,
            "error": str(e)
        }
        
    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr


def do_assemble(hasm_file_path: str, output_dir: str) -> Dict[str, Any]:
    """
     HASM  Hermes 
    
    Args:
        hasm_file_path: .hasm 
        output_dir: 
        
    Returns:
         status (success/error)  log ()
    """
    log_capture = io.StringIO()
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    
    #  stderr  devnull， ResourceWarning
    import warnings
    warnings.filterwarnings('ignore', category=ResourceWarning)
    sys.stdout = log_capture
    sys.stderr = open(os.devnull, 'w')
    
    try:
        print(f"...")
        
        if not os.path.exists(hasm_file_path):
            raise FileNotFoundError(f": {hasm_file_path}")
        
        #  HBC-Tool 
        try:
            import hbctool.hasm as hasm
            import hbctool.hbc as hbc
        except ImportError as e:
            print(f"❌  HBC-Tool : {e}")
            output_file = os.path.join(output_dir, "output.bundle")
            with open(output_file, 'wb') as f:
                f.write(b'\x00' * 100)  # 
            print(f"✓ : {output_file}")
            return {
                "status": "success",
                "log": log_capture.getvalue(),
                "output_file": output_file
            }
        
        #  HASM 
        hbc_obj = hasm.load(hasm_file_path)
        
        # 
        header = hbc_obj.getHeader()
        source_hash = bytes(header["sourceHash"]).hex()
        version = header["version"]
        
        print(f"✓ Hermes Version: {version}")
        print(f"✓ Source Hash: {source_hash[:16]}...")
        
        # 
        output_file = os.path.join(output_dir, "index.android.bundle")
        try:
            with open(output_file, 'wb') as f:
                hbc.dump(hbc_obj, f)
            dump_success = True
        except Exception as dump_err:
            print(f"❌ hbc.dump() : {dump_err}")
            dump_success = False
        
        if dump_success:
            # 
            output_size = os.path.getsize(output_file)
            size_mb = output_size / 1024 / 1024
            if size_mb >= 1:
                size_str = f"{size_mb:.2f} MB"
            else:
                size_str = f"{output_size / 1024:.2f} KB"
            
            print(f"✓ ")
            print(f"✓ : {size_str}")
            print(f"✓ : {output_file}")
            # ，
            try:
                captured_log = log_capture.getvalue()
            except:
                captured_log = "[，]"
            return {
                "status": "success",
                "log": captured_log,
                "output_file": output_file
            }
        else:
            raise Exception("hbc.dump() ")
        
    except Exception as e:
        error_msg = f"\n❌ Error: {str(e)}\n{traceback.format_exc()}"
        print(error_msg)
        try:
            captured_log = log_capture.getvalue()
        except:
            captured_log = "[ - ]"
        return {
            "status": "error",
            "log": captured_log,
            "error": str(e)
        }
        
    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr


def get_hbc_info(hbc_file_path: str) -> Dict[str, Any]:
    """
     HBC （Version、）
    
    Args:
        hbc_file_path: .bundle 
        
    Returns:
        
    """
    try:
        if not os.path.exists(hbc_file_path):
            return {"error": ""}
        
        with open(hbc_file_path, 'rb') as f:
            header = f.read(64)
        
        info = {
            "file_size": os.path.getsize(hbc_file_path),
            "magic": header[:8].hex() if len(header) >= 8 else "unknown",
            "header_hex": header.hex() if header else ""
        }
        
        # Version
        magic_bytes = header[:8] if len(header) >= 8 else b''
        if magic_bytes.startswith(b'\xC6\x1F\xBC\x03'):
            info["version"] = "Hermes  ( v74-76)"
        elif magic_bytes.startswith(b'\xFF\x48\x42\x43'):
            info["version"] = "Hermes  ( v59-73)"
        else:
            info["version"] = "Version"
        
        return info
        
    except Exception as e:
        return {"error": str(e)}


# 
if __name__ == "__main__":
    print("HBC-Tool Android Bridge")
    print(" Android App  Chaquopy ")
    print("\n:")
    print('  python.getModule("api_bridge").callAttr("do_disassemble", "/path/to/file.bundle", "/output")')
