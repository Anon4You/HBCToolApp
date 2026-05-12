"""
 HBC-Tool 

"""

import sys
import os

#  1: 
print("=" * 60)
print(" 1:  HBC-Tool ")
print("=" * 60)

try:
    import hbctool.hbc as hbc
    import hbctool.hasm as hasm
    print("✅ hbctool.hbc ")
    print("✅ hbctool.hasm ")
except ImportError as e:
    print(f"❌ : {e}")
    sys.exit(1)

#  2:  api_bridge 
print("\n" + "=" * 60)
print(" 2:  api_bridge ")
print("=" * 60)

try:
    import api_bridge
    print("✅ api_bridge ")
    print(f"   - do_disassemble: {hasattr(api_bridge, 'do_disassemble')}")
    print(f"   - do_assemble: {hasattr(api_bridge, 'do_assemble')}")
    print(f"   - get_hbc_info: {hasattr(api_bridge, 'get_hbc_info')}")
except ImportError as e:
    print(f"❌ : {e}")
    sys.exit(1)

#  3: 
print("\n" + "=" * 60)
print(" 3: ")
print("=" * 60)

dependencies = {
    'construct': 'construct',
    'colorama': 'colorama',
    'docopt': 'docopt'
}

for name, module in dependencies.items():
    try:
        __import__(module)
        print(f"✅ {name} ")
    except ImportError:
        print(f"⚠️  {name} （）")

#  4:  HBC Version
print("\n" + "=" * 60)
print(" 4:  Hermes Version")
print("=" * 60)

hbc_dir = os.path.join(os.path.dirname(__file__), 'hbctool', 'hbc')
versions = []
for item in os.listdir(hbc_dir):
    if item.startswith('hbc') and os.path.isdir(os.path.join(hbc_dir, item)):
        versions.append(item)

versions.sort()
print(f"Version: {', '.join(versions)}")
print(f": {len(versions)} Version")

print("\n" + "=" * 60)
print("✅ ！Starting build Android ")
print("=" * 60)
